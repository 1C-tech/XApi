package com.example.xapi.api;

import com.example.xapi.dto.TweetCommentsPage;
import com.example.xapi.dto.UserTweetsPage;
import com.example.xapi.service.XUserTweetsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/x")
public class XUserTweetsController {
    private final XUserTweetsService userTweetsService;

    public XUserTweetsController(XUserTweetsService userTweetsService) {
        this.userTweetsService = userTweetsService;
    }

    @GetMapping("/user-tweets")
    public UserTweetsPage userTweets(
            @RequestParam String userId,
            @RequestParam(defaultValue = "20") int count,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "false") boolean raw
    ) {
        return userTweetsService.fetchUserTweets(userId, count, cursor, raw);
    }

    @GetMapping("/tweet-comments")
    public TweetCommentsPage tweetComments(
            @RequestParam String tweetId,
            @RequestParam(defaultValue = "20") int count,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "false") boolean raw
    ) {
        return userTweetsService.fetchTweetComments(tweetId, count, cursor, raw);
    }
}

