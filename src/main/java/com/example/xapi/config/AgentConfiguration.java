package com.example.xapi.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AgentConfiguration {
    @Bean
    public RestTemplate agentRestTemplate(RestTemplateBuilder builder, AgentProperties properties) {
        return builder
                .setConnectTimeout(properties.getTimeout())
                .setReadTimeout(properties.getTimeout())
                .build();
    }
}
