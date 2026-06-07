package com.example.xapi.service;

import com.example.xapi.config.AgentProperties;
import com.example.xapi.dto.AgentAskRequest;
import com.example.xapi.dto.AgentAskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AgentProxyServiceTest {
    @Test
    void forwardsAskRequestToPythonAgent() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        AgentProperties properties = new AgentProperties();
        properties.setBaseUrl("http://localhost:9001");
        AgentProxyService service = new AgentProxyService(restTemplate, properties);

        server.expect(once(), requestTo("http://localhost:9001/ask"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.message").value("分析 AAPL 和 000001.SZ"))
                .andExpect(jsonPath("$.user_id").value("902839045356744704"))
                .andRespond(withSuccess("""
                        {
                          "answer": "ok",
                          "quotes": [{"market":"US","symbol":"AAPL","source":"alpha_vantage"}],
                          "posts": [],
                          "used_tradingagents": false,
                          "warnings": []
                        }
                        """, MediaType.APPLICATION_JSON));

        AgentAskResponse response = service.ask(
                new AgentAskRequest("分析 AAPL 和 000001.SZ", "902839045356744704", List.of("AAPL"))
        );

        assertThat(response.answer()).isEqualTo("ok");
        assertThat(response.quotes()).hasSize(1);
        assertThat(response.quotes().get(0).symbol()).isEqualTo("AAPL");
        server.verify();
    }

    @Test
    void forwardsAskRequestWithoutOptionalUserId() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        AgentProperties properties = new AgentProperties();
        properties.setBaseUrl("http://localhost:9001");
        AgentProxyService service = new AgentProxyService(restTemplate, properties);

        server.expect(once(), requestTo("http://localhost:9001/ask"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.message").value("分析 AAPL"))
                .andExpect(jsonPath("$.user_id").doesNotExist())
                .andRespond(withSuccess("""
                        {
                          "answer": "ok",
                          "quotes": [],
                          "posts": [],
                          "used_tradingagents": false,
                          "warnings": []
                        }
                        """, MediaType.APPLICATION_JSON));

        AgentAskResponse response = service.ask(new AgentAskRequest("分析 AAPL", null, null));

        assertThat(response.answer()).isEqualTo("ok");
        server.verify();
    }

    @Test
    void keepsAgentDefaults() {
        AgentProperties properties = new AgentProperties();

        assertThat(properties.getBaseUrl()).isEqualTo("http://localhost:9001");
        assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(120));
    }
}
