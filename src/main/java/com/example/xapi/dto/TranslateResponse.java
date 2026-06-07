package com.example.xapi.dto;

public record TranslateResponse(
        String translatedText,
        String sourceLang,
        String targetLang,
        String provider
) {
}
