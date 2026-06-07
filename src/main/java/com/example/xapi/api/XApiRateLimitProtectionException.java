package com.example.xapi.api;

public class XApiRateLimitProtectionException extends RuntimeException {
    private final Long resetEpochSeconds;

    public XApiRateLimitProtectionException(String message, Long resetEpochSeconds) {
        super(message);
        this.resetEpochSeconds = resetEpochSeconds;
    }

    public Long resetEpochSeconds() {
        return resetEpochSeconds;
    }
}

