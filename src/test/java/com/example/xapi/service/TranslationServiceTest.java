package com.example.xapi.service;

import com.example.xapi.config.TranslationProperties;
import com.example.xapi.dto.TranslateRequest;
import com.example.xapi.dto.TranslateResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadGateway;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TranslationServiceTest {
    @Test
    void translatesTextThroughConfiguredProvider() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TranslationProperties properties = new TranslationProperties();
        properties.setBaseUrl("https://translate.example/translate");
        properties.setApiKey("secret");
        TranslationService service = new TranslationService(restTemplate, properties);

        server.expect(once(), requestTo("https://translate.example/translate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "secret"))
                .andExpect(jsonPath("$.q").value("Markets are moving"))
                .andExpect(jsonPath("$.source").value("auto"))
                .andExpect(jsonPath("$.target").value("zh"))
                .andRespond(withSuccess("{\"translatedText\":\"市场正在波动\"}", MediaType.APPLICATION_JSON));

        TranslateResponse response = service.translate(
                new TranslateRequest("Markets are moving", "auto", "zh-CN")
        );

        assertThat(response.translatedText()).isEqualTo("市场正在波动");
        assertThat(response.sourceLang()).isEqualTo("auto");
        assertThat(response.targetLang()).isEqualTo("zh-CN");
        assertThat(response.provider()).isEqualTo("libretranslate");
        server.verify();
    }

    @Test
    void rejectsBlankText() {
        TranslationService service = new TranslationService(new RestTemplate(), new TranslationProperties());

        assertThatThrownBy(() -> service.translate(new TranslateRequest("  ", "auto", "zh-CN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("text must not be blank");
    }

    @Test
    void fallsBackToMyMemoryWhenPrimaryProviderFails() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TranslationProperties properties = new TranslationProperties();
        properties.setBaseUrl("https://translate.example/translate");
        TranslationService service = new TranslationService(restTemplate, properties);

        server.expect(once(), requestTo("https://translate.example/translate"))
                .andRespond(withBadGateway());
        server.expect(once(), requestTo("https://api.mymemory.translated.net/get?q=Markets%20are%20moving&langpair=en%7Czh-CN"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"responseData\":{\"translatedText\":\"市场正在变化\"},\"responseStatus\":200}",
                        MediaType.APPLICATION_JSON
                ));

        TranslateResponse response = service.translate(
                new TranslateRequest("Markets are moving", "en", "zh-CN")
        );

        assertThat(response.translatedText()).isEqualTo("市场正在变化");
        assertThat(response.provider()).isEqualTo("mymemory");
        server.verify();
    }

    @Test
    void keepsReasonableDefaults() {
        TranslationProperties properties = new TranslationProperties();

        assertThat(properties.getBaseUrl()).isEqualTo("https://libretranslate.com/translate");
        assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(15));
    }
}
