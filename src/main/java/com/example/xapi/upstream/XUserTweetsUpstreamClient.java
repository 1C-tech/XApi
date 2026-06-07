package com.example.xapi.upstream;

import com.example.xapi.dto.UserTweetsPage;

public interface XUserTweetsUpstreamClient {
    UserTweetsPage fetchUserTweets(String userId, int count, String cursor);
}

