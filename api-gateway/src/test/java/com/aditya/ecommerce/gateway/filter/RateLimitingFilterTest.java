package com.aditya.ecommerce.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingFilterTest {

    private final RateLimitingFilter filter = new RateLimitingFilter();

    @Test
    void allSixtyRequestsInTheWindowFromTheSameClientReachTheChain() {
        AtomicInteger chainInvocations = new AtomicInteger();
        GatewayFilterChain chain = ex -> {
            chainInvocations.incrementAndGet();
            return Mono.empty();
        };

        for (int i = 0; i < 60; i++) {
            MockServerWebExchange exchange = exchangeFrom("127.0.0.1", 10_000 + i);
            filter.filter(exchange, chain).block();
            assertThat(exchange.getResponse().getStatusCode()).isNull();
        }

        assertThat(chainInvocations.get()).isEqualTo(60);
    }

    @Test
    void theSixtyFirstRequestFromTheSameClientIsRejected() {
        GatewayFilterChain noopChain = ex -> Mono.empty();
        for (int i = 0; i < 60; i++) {
            filter.filter(exchangeFrom("127.0.0.1", 10_000 + i), noopChain).block();
        }

        AtomicInteger chainInvocations = new AtomicInteger();
        GatewayFilterChain trackingChain = ex -> {
            chainInvocations.incrementAndGet();
            return Mono.empty();
        };
        MockServerWebExchange exchange61 = exchangeFrom("127.0.0.1", 20_000);

        filter.filter(exchange61, trackingChain).block();

        assertThat(exchange61.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(chainInvocations.get()).isZero();
    }

    @Test
    void aDifferentClientIsUnaffectedByAnotherClientsCount() {
        GatewayFilterChain noopChain = ex -> Mono.empty();
        for (int i = 0; i < 61; i++) {
            filter.filter(exchangeFrom("127.0.0.1", 10_000 + i), noopChain).block();
        }

        AtomicInteger chainInvocations = new AtomicInteger();
        GatewayFilterChain trackingChain = ex -> {
            chainInvocations.incrementAndGet();
            return Mono.empty();
        };
        MockServerWebExchange otherClientExchange = exchangeFrom("10.0.0.5", 30_000);

        filter.filter(otherClientExchange, trackingChain).block();

        assertThat(otherClientExchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(chainInvocations.get()).isEqualTo(1);
    }

    private MockServerWebExchange exchangeFrom(String ip, int port) {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products")
                .remoteAddress(new InetSocketAddress(ip, port))
                .build();
        return MockServerWebExchange.from(request);
    }
}
