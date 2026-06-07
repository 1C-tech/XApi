package com.example.xapi.cache;

import com.example.xapi.dto.UserTweetsPage;
import com.example.xapi.dto.TweetCommentsPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RedisXUserTweetsCacheStore implements XUserTweetsCacheStore {
    private static final RedisScript<Long> UNLOCK_SCRIPT = RedisScript.of(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisXUserTweetsCacheStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<CachedUserTweetsPage> getFresh(String key) {
        return getPage(XUserTweetsCacheKey.fresh(key));
    }

    @Override
    public Optional<CachedUserTweetsPage> getStale(String key) {
        return getPage(XUserTweetsCacheKey.stale(key));
    }

    @Override
    public void put(String key, UserTweetsPage page, Duration freshTtl, Duration staleTtl) {
        putJson(XUserTweetsCacheKey.fresh(key), page, freshTtl);
        putJson(XUserTweetsCacheKey.stale(key), page, staleTtl);
    }

    @Override
    public Optional<CachedTweetCommentsPage> getFreshComments(String key) {
        return getCommentsPage(XUserTweetsCacheKey.fresh(key));
    }

    @Override
    public Optional<CachedTweetCommentsPage> getStaleComments(String key) {
        return getCommentsPage(XUserTweetsCacheKey.stale(key));
    }

    @Override
    public void putComments(String key, TweetCommentsPage page, Duration freshTtl, Duration staleTtl) {
        putJson(XUserTweetsCacheKey.fresh(key), page, freshTtl);
        putJson(XUserTweetsCacheKey.stale(key), page, staleTtl);
    }

    @Override
    public Optional<XApiRateLimitState> getRateLimitState() {
        String json = redisTemplate.opsForValue().get(XUserTweetsCacheKey.rateLimit());
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, XApiRateLimitState.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize X rate limit state", e);
        }
    }

    @Override
    public void putRateLimitState(XApiRateLimitState state) {
        putJson(XUserTweetsCacheKey.rateLimit(), state, Duration.ofHours(2));
    }

    @Override
    public Optional<DistributedLock> tryLock(String key, Duration ttl) {
        String value = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(acquired)
                ? Optional.of(new DistributedLock(key, value))
                : Optional.empty();
    }

    @Override
    public void unlock(DistributedLock lock) {
        redisTemplate.execute(UNLOCK_SCRIPT, List.of(lock.key()), lock.value());
    }

    private Optional<CachedUserTweetsPage> getPage(String redisKey) {
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            return Optional.empty();
        }
        try {
            UserTweetsPage page = objectMapper.readValue(json, UserTweetsPage.class);
            Long ttl = redisTemplate.getExpire(redisKey);
            return Optional.of(new CachedUserTweetsPage(page, ttl == null ? -1 : ttl));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cached user tweets", e);
        }
    }

    private Optional<CachedTweetCommentsPage> getCommentsPage(String redisKey) {
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            return Optional.empty();
        }
        try {
            TweetCommentsPage page = objectMapper.readValue(json, TweetCommentsPage.class);
            Long ttl = redisTemplate.getExpire(redisKey);
            return Optional.of(new CachedTweetCommentsPage(page, ttl == null ? -1 : ttl));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cached tweet comments", e);
        }
    }

    private void putJson(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Redis value: " + e.getOriginalMessage(), e);
        }
    }
}

