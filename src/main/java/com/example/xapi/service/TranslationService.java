package com.example.xapi.service;

import com.example.xapi.api.XApiRequestException;
import com.example.xapi.cache.TranslationCacheKey;
import com.example.xapi.cache.TranslationCacheStore;
import com.example.xapi.config.TranslationProperties;
import com.example.xapi.dto.TranslateRequest;
import com.example.xapi.dto.TranslateResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

@Service
public class TranslationService {
    private static final String PRIMARY_PROVIDER = "libretranslate";
    private static final String FALLBACK_PROVIDER = "mymemory";
    private static final String MY_MEMORY_URL = "https://api.mymemory.translated.net/get";

    private final RestTemplate restTemplate;
    private final TranslationProperties properties;
    private final TranslationCacheStore cacheStore;

    @Autowired
    public TranslationService(
            RestTemplate restTemplate,
            TranslationProperties properties,
            TranslationCacheStore cacheStore
    ) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.cacheStore = cacheStore;
    }

    public TranslationService(RestTemplate restTemplate, TranslationProperties properties) {
        this(restTemplate, properties, new NoopTranslationCacheStore());
    }

    public TranslateResponse translate(TranslateRequest request) {
        if (request == null || !StringUtils.hasText(request.text())) {
            throw new IllegalArgumentException("text must not be blank");
        }

        String sourceLang = StringUtils.hasText(request.sourceLang()) ? request.sourceLang() : "auto";
        String targetLang = StringUtils.hasText(request.targetLang()) ? request.targetLang() : "zh-CN";
        String cacheKey = TranslationCacheKey.translation(sourceLang, targetLang, request.text());
        Optional<TranslateResponse> cachedResponse = cacheStore.get(cacheKey);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }

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
            TranslateResponse response = translateWithLibreTranslate(body, headers, sourceLang, targetLang);
            cacheStore.put(cacheKey, response, properties.getCacheTtl());
            return response;
        } catch (RestClientException | XApiRequestException primaryFailure) {
            try {
                TranslateResponse response = translateWithMyMemory(request.text(), sourceLang, targetLang);
                cacheStore.put(cacheKey, response, properties.getCacheTtl());
                return response;
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

    private static final class NoopTranslationCacheStore implements TranslationCacheStore {
        @Override
        public Optional<TranslateResponse> get(String key) {
            return Optional.empty();
        }

        @Override
        public void put(String key, TranslateResponse response, Duration ttl) {
        }
    }
}
