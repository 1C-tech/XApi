package com.example.xapi.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class XUserTweetsCacheKey {
    private XUserTweetsCacheKey() {
    }

    public static String userTweets(String userId, int count, String cursor, boolean raw) {
        return "x:user-tweets:" + userId + ":" + count + ":" + hash(cursor == null ? "" : cursor) + ":" + raw;
    }

    public static String lock(String cacheKey) {
        return "x:lock:" + cacheKey;
    }

    public static String fresh(String cacheKey) {
        return cacheKey + ":fresh";
    }

    public static String stale(String cacheKey) {
        return cacheKey + ":stale";
    }

    public static String rateLimit() {
        return "x:rate-limit:UserTweets";
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

