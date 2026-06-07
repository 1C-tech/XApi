package com.example.xapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TweetDto {
    private final String id;
    private final String createdAt;
    private final String fullText;
    private final String lang;
    private final int favoriteCount;
    private final int retweetCount;
    private final int replyCount;
    private final int quoteCount;
    private final int bookmarkCount;
    private final String viewCount;
    private final String authorName;
    private final String authorScreenName;
    private final String authorAvatarUrl;
    private final JsonNode raw;

    @JsonCreator
    public TweetDto(
            @JsonProperty("id") String id,
            @JsonProperty("createdAt") String createdAt,
            @JsonProperty("fullText") String fullText,
            @JsonProperty("lang") String lang,
            @JsonProperty("favoriteCount") int favoriteCount,
            @JsonProperty("retweetCount") int retweetCount,
            @JsonProperty("replyCount") int replyCount,
            @JsonProperty("quoteCount") int quoteCount,
            @JsonProperty("bookmarkCount") int bookmarkCount,
            @JsonProperty("viewCount") String viewCount,
            @JsonProperty("authorName") String authorName,
            @JsonProperty("authorScreenName") String authorScreenName,
            @JsonProperty("authorAvatarUrl") String authorAvatarUrl,
            @JsonProperty("raw") JsonNode raw
    ) {
        this.id = id;
        this.createdAt = createdAt;
        this.fullText = fullText;
        this.lang = lang;
        this.favoriteCount = favoriteCount;
        this.retweetCount = retweetCount;
        this.replyCount = replyCount;
        this.quoteCount = quoteCount;
        this.bookmarkCount = bookmarkCount;
        this.viewCount = viewCount;
        this.authorName = authorName;
        this.authorScreenName = authorScreenName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.raw = raw;
    }

    public String getId() {
        return id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getFullText() {
        return fullText;
    }

    public String getLang() {
        return lang;
    }

    public int getFavoriteCount() {
        return favoriteCount;
    }

    public int getRetweetCount() {
        return retweetCount;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public int getQuoteCount() {
        return quoteCount;
    }

    public int getBookmarkCount() {
        return bookmarkCount;
    }

    public String getViewCount() {
        return viewCount;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorScreenName() {
        return authorScreenName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public JsonNode getRaw() {
        return raw;
    }

    public TweetDto withoutRaw() {
        return new TweetDto(
                id,
                createdAt,
                fullText,
                lang,
                favoriteCount,
                retweetCount,
                replyCount,
                quoteCount,
                bookmarkCount,
                viewCount,
                authorName,
                authorScreenName,
                authorAvatarUrl,
                null
        );
    }
}

