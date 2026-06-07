package com.example.xapi.service;

import com.example.xapi.api.XApiRequestException;
import com.example.xapi.config.TranslationProperties;
import com.example.xapi.dto.TranslateRequest;
import com.example.xapi.dto.TranslateResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class TranslationService {
    private static final String PRIMARY_PROVIDER = "libretranslate";
    private static final String FALLBACK_PROVIDER = "mymemory";
    private static final String MY_MEMORY_URL = "https://api.mymemory.translated.net/get";

    private final RestTemplate restTemplate;
    private final TranslationProperties properties;

    public TranslationService(RestTemplate restTemplate, TranslationProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public TranslateResponse translate(TranslateRequest request) {
        if (request == null || !StringUtils.hasText(request.text())) {
            throw new IllegalArgumentException("text must not be blank");
        }

        String sourceLang = StringUtils.hasText(request.sourceLang()) ? request.sourceLang() : "auto";
        String targetLang = StringUtils.hasText(request.targetLang()) ? request.targetLang() : "zh-CN";

        LibreTranslateRequest body = new LibreTranslateRequest(
                request.text(),
                toProviderLanguage(sourceLang),
                toProviderLanguage(targetLang)
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(properties.getApiKey())) {
            headers.add("x-api-key", properties.getApiKey());
        }

        try {
            return translateWithLibreTranslate(body, headers, sourceLang, targetLang);
        } catch (RestClientException | XApiRequestException primaryFailure) {
            try {
                return translateWithMyMemory(request.text(), sourceLang, targetLang);
            } catch (RestClientException | XApiRequestException fallbackFailure) {
                throw new XApiRequestException("Translation request failed", primaryFailure);
            }
        }
    }

    private TranslateResponse translateWithLibreTranslate(
            LibreTranslateRequest body,
            HttpHeaders headers,
            String sourceLang,
            String targetLang
    ) {
        LibreTranslateResponse response = restTemplate.postForObject(
                properties.getBaseUrl(),
                new HttpEntity<>(body, headers),
                LibreTranslateResponse.class
        );
        if (response == null || !StringUtils.hasText(response.translatedText())) {
            throw new XApiRequestException("Translation provider returned empty response");
        }
        return new TranslateResponse(response.translatedText(), sourceLang, targetLang, PRIMARY_PROVIDER);
    }

    private TranslateResponse translateWithMyMemory(String text, String sourceLang, String targetLang) {
        if ("auto".equalsIgnoreCase(sourceLang)) {
            throw new XApiRequestException("Fallback translation requires a source language");
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(MY_MEMORY_URL)
                .queryParam("q", text)
                .queryParam("langpair", toMyMemoryLanguage(sourceLang) + "|" + toMyMemoryLanguage(targetLang))
                .build()
                .encode()
                .toUri();
        MyMemoryResponse response = restTemplate.getForObject(uri, MyMemoryResponse.class);
        if (response == null
                || response.responseData() == null
                || !StringUtils.hasText(response.responseData().translatedText())) {
            throw new XApiRequestException("Fallback translation provider returned empty response");
        }
        if (response.responseStatus() != null && response.responseStatus() >= 400) {
            throw new HttpServerErrorException(org.springframework.http.HttpStatus.BAD_GATEWAY);
        }
        return new TranslateResponse(
                response.responseData().translatedText(),
                sourceLang,
                targetLang,
                FALLBACK_PROVIDER
        );
    }

    private record LibreTranslateRequest(
            @JsonProperty("q") String text,
            @JsonProperty("source") String sourceLang,
            @JsonProperty("target") String targetLang
    ) {
    }

    private record LibreTranslateResponse(
            String translatedText
    ) {
    }

    private record MyMemoryResponse(
            MyMemoryResponseData responseData,
            Integer responseStatus
    ) {
    }

    private record MyMemoryResponseData(
            String translatedText
    ) {
    }

    private static String toProviderLanguage(String language) {
        if ("zh-CN".equalsIgnoreCase(language) || "zh-Hans".equalsIgnoreCase(language)) {
            return "zh";
        }
        return language;
    }

    private static String toMyMemoryLanguage(String language) {
        if ("zh".equalsIgnoreCase(language) || "zh-Hans".equalsIgnoreCase(language)) {
            return "zh-CN";
        }
        return language;
    }
}
