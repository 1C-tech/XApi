package com.example.xapi.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TranslationConfiguration {
    @Bean
    public RestTemplate translationRestTemplate(
            RestTemplateBuilder builder,
            TranslationProperties properties
    ) {
        return builder
                .setConnectTimeout(properties.getTimeout())
                .setReadTimeout(properties.getTimeout())
                .build();
    }
}
