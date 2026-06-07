package com.example.xapi.dto;

public record TranslateRequest(
        String text,
        String sourceLang,
        String targetLang
) {
}
