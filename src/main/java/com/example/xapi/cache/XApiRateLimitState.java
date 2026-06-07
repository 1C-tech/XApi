package com.example.xapi.cache;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class XApiRateLimitState {
    private final Integer limit;
    private final Integer remaining;
    private final Long resetEpochSeconds;
    private final Instant updatedAt;

    @JsonCreator
    public XApiRateLimitState(
            @JsonProperty("limit") Integer limit,
            @JsonProperty("remaining") Integer remaining,
            @JsonProperty("resetEpochSeconds") Long resetEpochSeconds,
            @JsonProperty("updatedAt") Instant updatedAt
    ) {
        this.limit = limit;
        this.remaining = remaining;
        this.resetEpochSeconds = resetEpochSeconds;
        this.updatedAt = updatedAt;
    }

    public Integer limit() {
        return limit;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer remaining() {
        return remaining;
    }

    public Integer getRemaining() {
        return remaining;
    }

    public Long resetEpochSeconds() {
        return resetEpochSeconds;
    }

    public Long getResetEpochSeconds() {
        return resetEpochSeconds;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

