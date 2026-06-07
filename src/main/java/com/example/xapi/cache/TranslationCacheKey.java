package com.example.xapi.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class TranslationCacheKey {
    private static final String VERSION = "v1";

    private TranslationCacheKey() {
    }

    public static String translation(String sourceLang, String targetLang, String text) {
        return "translation:" + VERSION + ":"
                + normalizeLanguage(sourceLang) + ":"
                + normalizeLanguage(targetLang) + ":"
                + hash(text);
    }

    private static String normalizeLanguage(String language) {
        return language.toLowerCase(Locale.ROOT);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
