package com.example.xapi.service;

import com.example.xapi.api.XApiRateLimitProtectionException;
import com.example.xapi.api.XApiRefreshInProgressException;
import com.example.xapi.cache.CachedTweetCommentsPage;
import com.example.xapi.cache.CachedUserTweetsPage;
import com.example.xapi.cache.DistributedLock;
import com.example.xapi.cache.XApiRateLimitState;
import com.example.xapi.cache.XUserTweetsCacheKey;
import com.example.xapi.cache.XUserTweetsCacheStore;
import com.example.xapi.config.XUserTweetsProperties;
import com.example.xapi.dto.CacheMetadataDto;
import com.example.xapi.dto.RateLimitDto;
import com.example.xapi.dto.TweetCommentsPage;
import com.example.xapi.dto.UserTweetsPage;
import com.example.xapi.upstream.XUserTweetsUpstreamClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class XUserTweetsService {
    private final XUserTweetsUpstreamClient upstreamClient;
    private final XUserTweetsCacheStore cacheStore;
    private final XUserTweetsProperties properties;
    private final Clock clock;

    @Autowired
    public XUserTweetsService(
            XUserTweetsUpstreamClient upstreamClient,
            XUserTweetsCacheStore cacheStore,
            XUserTweetsProperties properties
    ) {
        this(upstreamClient, cacheStore, properties, Clock.systemUTC());
    }

    XUserTweetsService(
            XUserTweetsUpstreamClient upstreamClient,
            XUserTweetsCacheStore cacheStore,
            XUserTweetsProperties properties,
            Clock clock
    ) {
        this.upstreamClient = upstreamClient;
        this.cacheStore = cacheStore;
        this.properties = properties;
        this.clock = clock;
    }

    public UserTweetsPage fetchUserTweets(String userId, int count) {
        return fetchUserTweets(userId, count, null, false);
    }

    public UserTweetsPage fetchUserTweets(String userId, int count, String cursor) {
        return fetchUserTweets(userId, count, cursor, false);
    }

    public UserTweetsPage fetchUserTweets(String userId, int count, String cursor, boolean raw) {
        validateRequest(userId, count);
        String cacheKey = XUserTweetsCacheKey.userTweets(userId, count, cursor, raw);

        Optional<CachedUserTweetsPage> fresh = cacheStore.getFresh(cacheKey);
        if (fresh.isPresent()) {
            return withCache(fresh.get(), cacheKey, true, false);
        }

        if (rateLimitProtectionActive()) {
            return staleOrRateLimited(cacheKey);
        }

        String lockKey = XUserTweetsCacheKey.lock(cacheKey);
        Optional<DistributedLock> lock = cacheStore.tryLock(lockKey, properties.getLockTtl());
        if (lock.isEmpty()) {
            return waitForRefreshOrFallback(cacheKey);
        }

        try {
            fresh = cacheStore.getFresh(cacheKey);
            if (fresh.isPresent()) {
                return withCache(fresh.get(), cacheKey, true, false);
            }

            UserTweetsPage upstreamPage = upstreamClient.fetchUserTweets(userId, count, cursor);
            if (!raw) {
                upstreamPage = upstreamPage.withoutRaw();
            }
            updateRateLimitState(upstreamPage.getRateLimit());
            cacheStore.put(cacheKey, upstreamPage, properties.getCacheTtl(), properties.getStaleTtl());
            return upstreamPage.withCache(new CacheMetadataDto(false, false, properties.getCacheTtl().toSeconds(), cacheKey));
        } catch (RuntimeException e) {
            Optional<CachedUserTweetsPage> stale = cacheStore.getStale(cacheKey);
            if (stale.isPresent()) {
                return withCache(stale.get(), cacheKey, true, true);
            }
            throw e;
        } finally {
            cacheStore.unlock(lock.get());
        }
    }

    public TweetCommentsPage fetchTweetComments(String tweetId, int count, String cursor, boolean raw) {
        validateRequest(tweetId, count);
        String cacheKey = XUserTweetsCacheKey.tweetComments(tweetId, count, cursor, raw);

        Optional<CachedTweetCommentsPage> fresh = cacheStore.getFreshComments(cacheKey);
        if (fresh.isPresent()) {
            return withCommentsCache(fresh.get(), cacheKey, true, false);
        }

        if (rateLimitProtectionActive()) {
            return staleCommentsOrRateLimited(cacheKey);
        }

        String lockKey = XUserTweetsCacheKey.lock(cacheKey);
        Optional<DistributedLock> lock = cacheStore.tryLock(lockKey, properties.getLockTtl());
        if (lock.isEmpty()) {
            return waitForCommentsRefreshOrFallback(cacheKey);
        }

        try {
            fresh = cacheStore.getFreshComments(cacheKey);
            if (fresh.isPresent()) {
                return withCommentsCache(fresh.get(), cacheKey, true, false);
            }

            TweetCommentsPage upstreamPage = upstreamClient.fetchTweetComments(tweetId, count, cursor);
            if (!raw) {
                upstreamPage = upstreamPage.withoutRaw();
            }
            updateRateLimitState(upstreamPage.getRateLimit());
            cacheStore.putComments(cacheKey, upstreamPage, properties.getCacheTtl(), properties.getStaleTtl());
            return upstreamPage.withCache(new CacheMetadataDto(false, false, properties.getCacheTtl().toSeconds(), cacheKey));
        } catch (RuntimeException e) {
            Optional<CachedTweetCommentsPage> stale = cacheStore.getStaleComments(cacheKey);
            if (stale.isPresent()) {
                return withCommentsCache(stale.get(), cacheKey, true, true);
            }
            throw e;
        } finally {
            cacheStore.unlock(lock.get());
        }
    }

    private void validateRequest(String userId, int count) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (count < 1 || count > 100) {
            throw new IllegalArgumentException("count must be between 1 and 100");
        }
    }

    private boolean rateLimitProtectionActive() {
        Optional<XApiRateLimitState> state = cacheStore.getRateLimitState();
        if (state.isEmpty()) {
            return false;
        }
        XApiRateLimitState rateLimit = state.get();
        if (rateLimit.remaining() == null || rateLimit.resetEpochSeconds() == null) {
            return false;
        }
        boolean lowRemaining = rateLimit.remaining() <= properties.getRateLimitMinRemaining();
        Instant protectedUntil = Instant.ofEpochSecond(rateLimit.resetEpochSeconds())
                .plus(properties.getRateLimitSafetyWindow());
        return lowRemaining && clock.instant().isBefore(protectedUntil);
    }

    private UserTweetsPage staleOrRateLimited(String cacheKey) {
        Optional<CachedUserTweetsPage> stale = cacheStore.getStale(cacheKey);
        if (stale.isPresent()) {
            return withCache(stale.get(), cacheKey, true, true);
        }
        Long reset = cacheStore.getRateLimitState()
                .map(XApiRateLimitState::resetEpochSeconds)
                .orElse(null);
        throw new XApiRateLimitProtectionException("X API rate limit protection active", reset);
    }

    private TweetCommentsPage staleCommentsOrRateLimited(String cacheKey) {
        Optional<CachedTweetCommentsPage> stale = cacheStore.getStaleComments(cacheKey);
        if (stale.isPresent()) {
            return withCommentsCache(stale.get(), cacheKey, true, true);
        }
        Long reset = cacheStore.getRateLimitState()
                .map(XApiRateLimitState::resetEpochSeconds)
                .orElse(null);
        throw new XApiRateLimitProtectionException("X API rate limit protection active", reset);
    }

    private UserTweetsPage waitForRefreshOrFallback(String cacheKey) {
        long deadline = System.nanoTime() + properties.getLockWait().toNanos();
        while (System.nanoTime() < deadline) {
            Optional<CachedUserTweetsPage> fresh = cacheStore.getFresh(cacheKey);
            if (fresh.isPresent()) {
                return withCache(fresh.get(), cacheKey, true, false);
            }
            sleepQuietly(100);
        }
        Optional<CachedUserTweetsPage> stale = cacheStore.getStale(cacheKey);
        if (stale.isPresent()) {
            return withCache(stale.get(), cacheKey, true, true);
        }
        throw new XApiRefreshInProgressException("Upstream refresh in progress");
    }

    private TweetCommentsPage waitForCommentsRefreshOrFallback(String cacheKey) {
        long deadline = System.nanoTime() + properties.getLockWait().toNanos();
        while (System.nanoTime() < deadline) {
            Optional<CachedTweetCommentsPage> fresh = cacheStore.getFreshComments(cacheKey);
            if (fresh.isPresent()) {
                return withCommentsCache(fresh.get(), cacheKey, true, false);
            }
            sleepQuietly(100);
        }
        Optional<CachedTweetCommentsPage> stale = cacheStore.getStaleComments(cacheKey);
        if (stale.isPresent()) {
            return withCommentsCache(stale.get(), cacheKey, true, true);
        }
        throw new XApiRefreshInProgressException("Upstream refresh in progress");
    }

    private void updateRateLimitState(RateLimitDto rateLimit) {
        if (rateLimit == null) {
            return;
        }
        if (rateLimit.getLimit() == null && rateLimit.getRemaining() == null && rateLimit.getResetEpochSeconds() == null) {
            return;
        }
        cacheStore.putRateLimitState(new XApiRateLimitState(
                rateLimit.getLimit(),
                rateLimit.getRemaining(),
                rateLimit.getResetEpochSeconds(),
                clock.instant()
        ));
    }

    private static UserTweetsPage withCache(CachedUserTweetsPage cached, String cacheKey, boolean hit, boolean stale) {
        return cached.page().withCache(new CacheMetadataDto(hit, stale, cached.ttlSeconds(), cacheKey));
    }

    private static TweetCommentsPage withCommentsCache(CachedTweetCommentsPage cached, String cacheKey, boolean hit, boolean stale) {
        return cached.page().withCache(new CacheMetadataDto(hit, stale, cached.ttlSeconds(), cacheKey));
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new XApiRefreshInProgressException("Interrupted while waiting for upstream refresh");
        }
    }
}

