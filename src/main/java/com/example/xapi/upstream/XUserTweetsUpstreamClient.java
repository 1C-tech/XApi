package com.example.xapi.upstream;

import com.example.xapi.dto.UserTweetsPage;
import com.example.xapi.dto.TweetCommentsPage;

public interface XUserTweetsUpstreamClient {
    UserTweetsPage fetchUserTweets(String userId, int count, String cursor);

    TweetCommentsPage fetchTweetComments(String tweetId, int count, String cursor);
}

