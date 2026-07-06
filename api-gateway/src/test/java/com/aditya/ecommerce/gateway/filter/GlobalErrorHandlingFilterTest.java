package com.aditya.ecommerce.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalErrorHandlingFilterTest {

    private final GlobalErrorHandlingFilter filter = new GlobalErrorHandlingFilter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void timeoutExceptionIsMappedToGatewayTimeout() throws Exception {
        MockServerWebExchange exchange = exchange();
        GatewayFilterChain chain = ex -> Mono.error(new TimeoutException());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

        JsonNode body = readBody(exchange);
        assertThat(body.get("status").asInt()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
        assertThat(body.get("error").asText()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.getReasonPhrase());
        assertThat(body.get("message").asText()).isNotBlank();
    }

    @Test
    void connectExceptionIsMappedToBadGateway() throws Exception {
        MockServerWebExchange exchange = exchange();
        GatewayFilterChain chain = ex -> Mono.error(new ConnectException("connection refused"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

        JsonNode body = readBody(exchange);
        assertThat(body.get("status").asInt()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(body.get("error").asText()).isEqualTo(HttpStatus.BAD_GATEWAY.getReasonPhrase());
        assertThat(body.get("message").asText()).isNotBlank();
    }

    private JsonNode readBody(MockServerWebExchange exchange) throws Exception {
        MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
        String json = response.getBodyAsString().block();
        return objectMapper.readTree(json);
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders").build());
    }
}
