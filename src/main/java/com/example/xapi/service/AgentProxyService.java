package com.example.xapi.service;

import com.example.xapi.api.XApiRequestException;
import com.example.xapi.config.AgentProperties;
import com.example.xapi.dto.AgentAskRequest;
import com.example.xapi.dto.AgentAskResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentProxyService {
    private final RestTemplate agentRestTemplate;
    private final AgentProperties properties;

    public AgentProxyService(@Qualifier("agentRestTemplate") RestTemplate agentRestTemplate, AgentProperties properties) {
        this.agentRestTemplate = agentRestTemplate;
        this.properties = properties;
    }

    public AgentAskResponse ask(AgentAskRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new IllegalArgumentException("message must not be blank");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", request.message());
        if (StringUtils.hasText(request.userId())) {
            body.put("user_id", request.userId());
        }
        body.put("symbols", request.symbols() == null ? List.of() : request.symbols());
        try {
            return agentRestTemplate.postForObject(
                    stripTrailingSlash(properties.getBaseUrl()) + "/ask",
                    body,
                    AgentAskResponse.class
            );
        } catch (RestClientException e) {
            throw new XApiRequestException("Python agent request failed", e);
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
