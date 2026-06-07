package com.example.xapi.cache;

import com.example.xapi.dto.UserTweetsPage;
import com.example.xapi.dto.TweetCommentsPage;

import java.time.Duration;
import java.util.Optional;

public interface XUserTweetsCacheStore {
    Optional<CachedUserTweetsPage> getFresh(String key);

    Optional<CachedUserTweetsPage> getStale(String key);

    void put(String key, UserTweetsPage page, Duration freshTtl, Duration staleTtl);

    Optional<CachedTweetCommentsPage> getFreshComments(String key);

    Optional<CachedTweetCommentsPage> getStaleComments(String key);

    void putComments(String key, TweetCommentsPage page, Duration freshTtl, Duration staleTtl);

    Optional<XApiRateLimitState> getRateLimitState();

    void putRateLimitState(XApiRateLimitState state);

    Optional<DistributedLock> tryLock(String key, Duration ttl);

    void unlock(DistributedLock lock);
}

