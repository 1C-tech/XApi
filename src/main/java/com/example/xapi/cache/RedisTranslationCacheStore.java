package com.example.xapi.cache;

import com.example.xapi.dto.TranslateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisTranslationCacheStore implements TranslationCacheStore {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisTranslationCacheStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<TranslateResponse> get(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, TranslateResponse.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cached translation", e);
        }
    }

    @Override
    public void put(String key, TranslateResponse response, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(response), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize cached translation", e);
        }
    }
}
