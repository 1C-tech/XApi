package com.example.xapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class XApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(XApiApplication.class, args);
    }
}
