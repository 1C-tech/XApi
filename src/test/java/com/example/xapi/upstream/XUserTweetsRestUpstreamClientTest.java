package com.example.xapi.upstream;

import com.example.xapi.api.XApiRequestException;
import com.example.xapi.config.XUserTweetsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XUserTweetsRestUpstreamClientTest {
    @Test
    void fetchesTweetCommentsWithoutBearerToken() throws IOException {
        AtomicReference<List<String>> authorizationHeaders = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            authorizationHeaders.set(exchange.getRequestHeaders().get("authorization"));
            exchange.sendResponseHeaders(502, 0);
            exchange.close();
        });
        server.start();
        try {
            XUserTweetsProperties properties = new XUserTweetsProperties();
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            XUserTweetsRestUpstreamClient client = new XUserTweetsRestUpstreamClient(
                    new RestTemplateBuilder(),
                    new ObjectMapper(),
                    properties,
                    new XTimelineParser()
            );

            assertThatThrownBy(() -> client.fetchTweetComments("tweet-1", 20, null))
                    .isInstanceOf(XApiRequestException.class)
                    .hasMessageContaining("status=502");
            assertThat(authorizationHeaders.get()).isNull();
        } finally {
            server.stop(0);
        }
    }
}
