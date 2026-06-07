package com.example.xapi.service;

import com.example.xapi.cache.TranslationCacheStore;
import com.example.xapi.config.TranslationProperties;
import com.example.xapi.dto.TranslateRequest;
import com.example.xapi.dto.TranslateResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Optional;

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
    void returnsCachedTranslationWithoutCallingProvider() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TranslationProperties properties = new TranslationProperties();
        TestTranslationCacheStore cacheStore = new TestTranslationCacheStore();
        cacheStore.cachedResponse = new TranslateResponse("市场正在波动", "auto", "zh-CN", "libretranslate");
        TranslationService service = new TranslationService(restTemplate, properties, cacheStore);

        TranslateResponse response = service.translate(
                new TranslateRequest("Markets are moving", "auto", "zh-CN")
        );

        assertThat(response).isEqualTo(cacheStore.cachedResponse);
        assertThat(cacheStore.getKey).isEqualTo(
                "translation:v1:auto:zh-cn:5e5c1a817f66c0d170524a2b008efbbc6dec83193093c8c6db5786ffd6d8603b"
        );
        assertThat(cacheStore.putCount).isZero();
        server.verify();
    }

    @Test
    void cachesSuccessfulProviderResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TranslationProperties properties = new TranslationProperties();
        properties.setBaseUrl("https://translate.example/translate");
        properties.setCacheTtl(Duration.ofHours(12));
        TestTranslationCacheStore cacheStore = new TestTranslationCacheStore();
        TranslationService service = new TranslationService(restTemplate, properties, cacheStore);

        server.expect(once(), requestTo("https://translate.example/translate"))
                .andRespond(withSuccess("{\"translatedText\":\"市场正在波动\"}", MediaType.APPLICATION_JSON));

        TranslateResponse response = service.translate(
                new TranslateRequest("Markets are moving", "auto", "zh-CN")
        );

        assertThat(response.translatedText()).isEqualTo("市场正在波动");
        assertThat(cacheStore.putKey).isEqualTo(cacheStore.getKey);
        assertThat(cacheStore.putResponse).isEqualTo(response);
        assertThat(cacheStore.putTtl).isEqualTo(Duration.ofHours(12));
        assertThat(cacheStore.putCount).isEqualTo(1);
        server.verify();
    }

    @Test
    void doesNotCacheFailedTranslationResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TranslationProperties properties = new TranslationProperties();
        properties.setBaseUrl("https://translate.example/translate");
        TestTranslationCacheStore cacheStore = new TestTranslationCacheStore();
        TranslationService service = new TranslationService(restTemplate, properties, cacheStore);

        server.expect(once(), requestTo("https://translate.example/translate"))
                .andRespond(withBadGateway());

        assertThatThrownBy(() -> service.translate(new TranslateRequest("Markets are moving", "auto", "zh-CN")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Translation request failed");
        assertThat(cacheStore.putCount).isZero();
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
    void encodesSpecialCharactersWhenFallingBackToMyMemory() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TranslationProperties properties = new TranslationProperties();
        properties.setBaseUrl("https://translate.example/translate");
        TranslationService service = new TranslationService(restTemplate, properties);
        String text = "I now am the #1 most subscribed to account on the entire X platform! "
                + "After overtaking Elon Musk today. Thank you everyone for helping me achieve my goal. "
                + "https://t.co/XLjy4ZsZyD";

        server.expect(once(), requestTo("https://translate.example/translate"))
                .andRespond(withBadGateway());
        server.expect(once(), requestTo("https://api.mymemory.translated.net/get"
                        + "?q=I%20now%20am%20the%20%231%20most%20subscribed%20to%20account%20on%20the%20entire%20X%20platform!%20"
                        + "After%20overtaking%20Elon%20Musk%20today.%20Thank%20you%20everyone%20for%20helping%20me%20achieve%20my%20goal.%20"
                        + "https://t.co/XLjy4ZsZyD&langpair=en%7Czh-CN"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"responseData\":{\"translatedText\":\"已翻译\"},\"responseStatus\":200}",
                        MediaType.APPLICATION_JSON
                ));

        TranslateResponse response = service.translate(new TranslateRequest(text, "en", "zh-CN"));

        assertThat(response.translatedText()).isEqualTo("已翻译");
        assertThat(response.provider()).isEqualTo("mymemory");
        server.verify();
    }

    @Test
    void keepsReasonableDefaults() {
        TranslationProperties properties = new TranslationProperties();

        assertThat(properties.getBaseUrl()).isEqualTo("https://libretranslate.com/translate");
        assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(15));
        assertThat(properties.getCacheTtl()).isEqualTo(Duration.ofDays(7));
    }

    private static final class TestTranslationCacheStore implements TranslationCacheStore {
        private TranslateResponse cachedResponse;
        private String getKey;
        private String putKey;
        private TranslateResponse putResponse;
        private Duration putTtl;
        private int putCount;

        @Override
        public Optional<TranslateResponse> get(String key) {
            this.getKey = key;
            return Optional.ofNullable(cachedResponse);
        }

        @Override
        public void put(String key, TranslateResponse response, Duration ttl) {
            this.putKey = key;
            this.putResponse = response;
            this.putTtl = ttl;
            this.putCount++;
        }
    }
}
