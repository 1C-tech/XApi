package com.example.xapi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RateLimitDto {
    private final Integer limit;
    private final Integer remaining;
    private final Long resetEpochSeconds;

    @JsonCreator
    public RateLimitDto(
            @JsonProperty("limit") Integer limit,
            @JsonProperty("remaining") Integer remaining,
            @JsonProperty("resetEpochSeconds") Long resetEpochSeconds
    ) {
        this.limit = limit;
        this.remaining = remaining;
        this.resetEpochSeconds = resetEpochSeconds;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getRemaining() {
        return remaining;
    }

    public Long getResetEpochSeconds() {
        return resetEpochSeconds;
    }
}

