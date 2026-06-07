package com.example.xapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserTweetsPage {
    private final List<TweetDto> tweets;
    private final String nextCursor;
    private final RateLimitDto rateLimit;
    private final JsonNode raw;
    private final CacheMetadataDto cache;

    public UserTweetsPage(List<TweetDto> tweets, String nextCursor, RateLimitDto rateLimit, JsonNode raw) {
        this(tweets, nextCursor, rateLimit, raw, null);
    }

    @JsonCreator
    public UserTweetsPage(
            @JsonProperty("tweets") List<TweetDto> tweets,
            @JsonProperty("nextCursor") String nextCursor,
            @JsonProperty("rateLimit") RateLimitDto rateLimit,
            @JsonProperty("raw") JsonNode raw,
            @JsonProperty("cache") CacheMetadataDto cache
    ) {
        this.tweets = List.copyOf(tweets);
        this.nextCursor = nextCursor;
        this.rateLimit = rateLimit;
        this.raw = raw;
        this.cache = cache;
    }

    public List<TweetDto> getTweets() {
        return tweets;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public RateLimitDto getRateLimit() {
        return rateLimit;
    }

    public JsonNode getRaw() {
        return raw;
    }

    public CacheMetadataDto getCache() {
        return cache;
    }

    public UserTweetsPage withoutRaw() {
        return new UserTweetsPage(
                tweets.stream().map(TweetDto::withoutRaw).toList(),
                nextCursor,
                rateLimit,
                null,
                cache
        );
    }

    public UserTweetsPage withCache(CacheMetadataDto cache) {
        return new UserTweetsPage(tweets, nextCursor, rateLimit, raw, cache);
    }
}

