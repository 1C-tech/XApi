package com.example.xapi.cache;

public class DistributedLock {
    private final String key;
    private final String value;

    public DistributedLock(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String key() {
        return key;
    }

    public String value() {
        return value;
    }
}

