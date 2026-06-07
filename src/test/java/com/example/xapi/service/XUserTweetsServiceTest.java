package com.example.xapi.service;

import com.example.xapi.api.XApiRateLimitProtectionException;
import com.example.xapi.cache.CachedTweetCommentsPage;
import com.example.xapi.cache.CachedUserTweetsPage;
import com.example.xapi.cache.DistributedLock;
import com.example.xapi.cache.XApiRateLimitState;
import com.example.xapi.cache.XUserTweetsCacheKey;
import com.example.xapi.cache.XUserTweetsCacheStore;
import com.example.xapi.config.XUserTweetsProperties;
import com.example.xapi.dto.RateLimitDto;
import com.example.xapi.dto.TweetCommentsPage;
import com.example.xapi.dto.TweetDto;
import com.example.xapi.dto.UserTweetsPage;
import com.example.xapi.upstream.XUserTweetsUpstreamClient;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XUserTweetsServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1_700_000_000), ZoneOffset.UTC);

    @Test
    void returnsFreshCacheWithoutCallingUpstream() {
        FakeCacheStore cache = new FakeCacheStore();
        CountingUpstream upstream = new CountingUpstream(page("upstream", 49));
        XUserTweetsService service = service(cache, upstream);
        String key = XUserTweetsCacheKey.userTweets("u1", 20, null, false);
        cache.fresh.put(key, page("cached", 45));

        UserTweetsPage result = service.fetchUserTweets("u1", 20, null, false);

        assertThat(result.getTweets()).extracting(TweetDto::getId).containsExactly("cached");
        assertThat(result.getCache().isHit()).isTrue();
        assertThat(result.getCache().isStale()).isFalse();
        assertThat(upstream.calls()).isZero();
    }

    @Test
    void refreshesOnceWhenLockAcquiredAndStoresFreshAndStaleCache() {
        FakeCacheStore cache = new FakeCacheStore();
        CountingUpstream upstream = new CountingUpstream(page("upstream", 48));
        XUserTweetsService service = service(cache, upstream);

        UserTweetsPage result = service.fetchUserTweets("u1", 20, null, false);

        String key = XUserTweetsCacheKey.userTweets("u1", 20, null, false);
        assertThat(result.getTweets()).extracting(TweetDto::getId).containsExactly("upstream");
        assertThat(result.getCache().isHit()).isFalse();
        assertThat(cache.fresh).containsKey(key);
        assertThat(cache.stale).containsKey(key);
        assertThat(cache.rateLimitState.remaining()).isEqualTo(48);
        assertThat(upstream.calls()).isEqualTo(1);
    }

    @Test
    void returnsStaleCacheWhenRateLimitProtectionIsActive() {
        FakeCacheStore cache = new FakeCacheStore();
        CountingUpstream upstream = new CountingUpstream(page("upstream", 1));
        XUserTweetsService service = service(cache, upstream);
        String key = XUserTweetsCacheKey.userTweets("u1", 20, null, false);
        cache.stale.put(key, page("stale", 2));
        cache.rateLimitState = new XApiRateLimitState(50, 2, CLOCK.instant().plusSeconds(600).getEpochSecond(), CLOCK.instant());

        UserTweetsPage result = service.fetchUserTweets("u1", 20, null, false);

        assertThat(result.getTweets()).extracting(TweetDto::getId).containsExactly("stale");
        assertThat(result.getCache().isHit()).isTrue();
        assertThat(result.getCache().isStale()).isTrue();
        assertThat(upstream.calls()).isZero();
    }

    @Test
    void rejectsWhenRateLimitProtectionIsActiveAndNoStaleCacheExists() {
        FakeCacheStore cache = new FakeCacheStore();
        CountingUpstream upstream = new CountingUpstream(page("upstream", 1));
        XUserTweetsService service = service(cache, upstream);
        cache.rateLimitState = new XApiRateLimitState(50, 2, CLOCK.instant().plusSeconds(600).getEpochSecond(), CLOCK.instant());

        assertThatThrownBy(() -> service.fetchUserTweets("u1", 20, null, false))
                .isInstanceOf(XApiRateLimitProtectionException.class)
                .hasMessageContaining("rate limit protection active");
        assertThat(upstream.calls()).isZero();
    }

    @Test
    void returnsStaleCacheWhenAnotherInstanceIsRefreshing() {
        FakeCacheStore cache = new FakeCacheStore();
        cache.lockAvailable = false;
        CountingUpstream upstream = new CountingUpstream(page("upstream", 48));
        XUserTweetsService service = service(cache, upstream);
        String key = XUserTweetsCacheKey.userTweets("u1", 20, null, false);
        cache.stale.put(key, page("stale", 42));

        UserTweetsPage result = service.fetchUserTweets("u1", 20, null, false);

        assertThat(result.getTweets()).extracting(TweetDto::getId).containsExactly("stale");
        assertThat(result.getCache().isHit()).isTrue();
        assertThat(result.getCache().isStale()).isTrue();
        assertThat(upstream.calls()).isZero();
    }

    @Test
    void returnsFreshCachedTweetCommentsWithoutCallingUpstream() {
        FakeCacheStore cache = new FakeCacheStore();
        CountingUpstream upstream = new CountingUpstream(page("upstream", 49), commentsPage("upstream-comment", 49));
        XUserTweetsService service = service(cache, upstream);
        String key = XUserTweetsCacheKey.tweetComments("tweet-1", 20, null, false);
        cache.freshComments.put(key, commentsPage("cached-comment", 45));

        TweetCommentsPage result = service.fetchTweetComments("tweet-1", 20, null, false);

        assertThat(result.getComments()).extracting(TweetDto::getId).containsExactly("cached-comment");
        assertThat(result.getCache().isHit()).isTrue();
        assertThat(result.getCache().isStale()).isFalse();
        assertThat(upstream.commentCalls()).isZero();
    }

    @Test
    void refreshesTweetCommentsAndStoresFreshAndStaleCache() {
        FakeCacheStore cache = new FakeCacheStore();
        CountingUpstream upstream = new CountingUpstream(page("upstream", 48), commentsPage("upstream-comment", 47));
        XUserTweetsService service = service(cache, upstream);

        TweetCommentsPage result = service.fetchTweetComments("tweet-1", 20, null, false);

        String key = XUserTweetsCacheKey.tweetComments("tweet-1", 20, null, false);
        assertThat(result.getComments()).extracting(TweetDto::getId).containsExactly("upstream-comment");
        assertThat(result.getCache().isHit()).isFalse();
        assertThat(cache.freshComments).containsKey(key);
        assertThat(cache.staleComments).containsKey(key);
        assertThat(cache.rateLimitState.remaining()).isEqualTo(47);
        assertThat(upstream.commentCalls()).isEqualTo(1);
    }

    private static XUserTweetsService service(FakeCacheStore cache, CountingUpstream upstream) {
        XUserTweetsProperties properties = new XUserTweetsProperties();
        properties.setCacheTtl(Duration.ofMinutes(5));
        properties.setStaleTtl(Duration.ofMinutes(30));
        properties.setLockTtl(Duration.ofSeconds(15));
        properties.setLockWait(Duration.ofMillis(1));
        properties.setRateLimitMinRemaining(3);
        properties.setRateLimitSafetyWindow(Duration.ofSeconds(30));
        return new XUserTweetsService(upstream, cache, properties, CLOCK);
    }

    private static UserTweetsPage page(String id, int remaining) {
        TweetDto tweet = new TweetDto(id, "now", "text", "en", 1, 2, 3, 4, 5, "6", "name", "screen", "avatar", null);
        RateLimitDto rateLimit = new RateLimitDto(50, remaining, CLOCK.instant().plusSeconds(900).getEpochSecond());
        return new UserTweetsPage(java.util.List.of(tweet), "next", rateLimit, null);
    }

    private static TweetCommentsPage commentsPage(String id, int remaining) {
        TweetDto tweet = new TweetDto(id, "now", "comment", "en", 1, 2, 3, 4, 5, "6", "name", "screen", "avatar", null);
        RateLimitDto rateLimit = new RateLimitDto(50, remaining, CLOCK.instant().plusSeconds(900).getEpochSecond());
        return new TweetCommentsPage(java.util.List.of(tweet), "next", rateLimit, null);
    }

    private static final class CountingUpstream implements XUserTweetsUpstreamClient {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicInteger commentCalls = new AtomicInteger();
        private final UserTweetsPage page;
        private final TweetCommentsPage commentsPage;

        private CountingUpstream(UserTweetsPage page) {
            this(page, commentsPage("comment", 49));
        }

        private CountingUpstream(UserTweetsPage page, TweetCommentsPage commentsPage) {
            this.page = page;
            this.commentsPage = commentsPage;
        }

        @Override
        public UserTweetsPage fetchUserTweets(String userId, int count, String cursor) {
            calls.incrementAndGet();
            return page;
        }

        @Override
        public TweetCommentsPage fetchTweetComments(String tweetId, int count, String cursor) {
            commentCalls.incrementAndGet();
            return commentsPage;
        }

        private int calls() {
            return calls.get();
        }

        private int commentCalls() {
            return commentCalls.get();
        }
    }

    private static final class FakeCacheStore implements XUserTweetsCacheStore {
        private final Map<String, UserTweetsPage> fresh = new HashMap<>();
        private final Map<String, UserTweetsPage> stale = new HashMap<>();
        private final Map<String, TweetCommentsPage> freshComments = new HashMap<>();
        private final Map<String, TweetCommentsPage> staleComments = new HashMap<>();
        private boolean lockAvailable = true;
        private XApiRateLimitState rateLimitState;

        @Override
        public Optional<CachedUserTweetsPage> getFresh(String key) {
            return Optional.ofNullable(fresh.get(key)).map(page -> new CachedUserTweetsPage(page, 100));
        }

        @Override
        public Optional<CachedUserTweetsPage> getStale(String key) {
            return Optional.ofNullable(stale.get(key)).map(page -> new CachedUserTweetsPage(page, 100));
        }

        @Override
        public void put(String key, UserTweetsPage page, Duration freshTtl, Duration staleTtl) {
            fresh.put(key, page);
            stale.put(key, page);
        }

        @Override
        public Optional<CachedTweetCommentsPage> getFreshComments(String key) {
            return Optional.ofNullable(freshComments.get(key)).map(page -> new CachedTweetCommentsPage(page, 100));
        }

        @Override
        public Optional<CachedTweetCommentsPage> getStaleComments(String key) {
            return Optional.ofNullable(staleComments.get(key)).map(page -> new CachedTweetCommentsPage(page, 100));
        }

        @Override
        public void putComments(String key, TweetCommentsPage page, Duration freshTtl, Duration staleTtl) {
            freshComments.put(key, page);
            staleComments.put(key, page);
        }

        @Override
        public Optional<XApiRateLimitState> getRateLimitState() {
            return Optional.ofNullable(rateLimitState);
        }

        @Override
        public void putRateLimitState(XApiRateLimitState state) {
            this.rateLimitState = state;
        }

        @Override
        public Optional<DistributedLock> tryLock(String key, Duration ttl) {
            return lockAvailable ? Optional.of(new DistributedLock(key, "value")) : Optional.empty();
        }

        @Override
        public void unlock(DistributedLock lock) {
        }
    }
}
