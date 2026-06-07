package com.example.xapi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CacheMetadataDto {
    private final boolean hit;
    private final boolean stale;
    private final long ttlSeconds;
    private final String key;

    @JsonCreator
    public CacheMetadataDto(
            @JsonProperty("hit") boolean hit,
            @JsonProperty("stale") boolean stale,
            @JsonProperty("ttlSeconds") long ttlSeconds,
            @JsonProperty("key") String key
    ) {
        this.hit = hit;
        this.stale = stale;
        this.ttlSeconds = ttlSeconds;
        this.key = key;
    }

    public boolean isHit() {
        return hit;
    }

    public boolean isStale() {
        return stale;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public String getKey() {
        return key;
    }
}

