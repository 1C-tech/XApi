package com.example.xapi.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(XUserTweetsProperties.class)
public class XApiConfiguration {
}

