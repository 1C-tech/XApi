package com.example.xapi.cache;

import com.example.xapi.dto.TranslateResponse;

import java.time.Duration;
import java.util.Optional;

public interface TranslationCacheStore {
    Optional<TranslateResponse> get(String key);

    void put(String key, TranslateResponse response, Duration ttl);
}
