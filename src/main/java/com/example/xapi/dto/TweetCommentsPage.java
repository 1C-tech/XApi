package com.example.xapi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TweetCommentsPage {
    private final List<TweetDto> comments;
    private final String nextCursor;
    private final RateLimitDto rateLimit;
    private final JsonNode raw;
    private final CacheMetadataDto cache;

    public TweetCommentsPage(List<TweetDto> comments, String nextCursor, RateLimitDto rateLimit, JsonNode raw) {
        this(comments, nextCursor, rateLimit, raw, null);
    }

    @JsonCreator
    public TweetCommentsPage(
            @JsonProperty("comments") List<TweetDto> comments,
            @JsonProperty("nextCursor") String nextCursor,
            @JsonProperty("rateLimit") RateLimitDto rateLimit,
            @JsonProperty("raw") JsonNode raw,
            @JsonProperty("cache") CacheMetadataDto cache
    ) {
        this.comments = comments == null ? List.of() : List.copyOf(comments);
        this.nextCursor = nextCursor;
        this.rateLimit = rateLimit;
        this.raw = raw;
        this.cache = cache;
    }

    public List<TweetDto> getComments() {
        return comments;
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

    public TweetCommentsPage withoutRaw() {
        return new TweetCommentsPage(
                comments.stream().map(TweetDto::withoutRaw).toList(),
                nextCursor,
                rateLimit,
                null,
                cache
        );
    }

    public TweetCommentsPage withCache(CacheMetadataDto cache) {
        return new TweetCommentsPage(comments, nextCursor, rateLimit, raw, cache);
    }
}
