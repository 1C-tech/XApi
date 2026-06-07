package com.example.xapi.cache;

import com.example.xapi.dto.TweetCommentsPage;

public class CachedTweetCommentsPage {
    private final TweetCommentsPage page;
    private final long ttlSeconds;

    public CachedTweetCommentsPage(TweetCommentsPage page, long ttlSeconds) {
        this.page = page;
        this.ttlSeconds = ttlSeconds;
    }

    public TweetCommentsPage page() {
        return page;
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }
}
