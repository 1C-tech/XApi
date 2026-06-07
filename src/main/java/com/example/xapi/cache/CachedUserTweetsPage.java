package com.example.xapi.cache;

import com.example.xapi.dto.UserTweetsPage;

public class CachedUserTweetsPage {
    private final UserTweetsPage page;
    private final long ttlSeconds;

    public CachedUserTweetsPage(UserTweetsPage page, long ttlSeconds) {
        this.page = page;
        this.ttlSeconds = ttlSeconds;
    }

    public UserTweetsPage page() {
        return page;
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }
}

