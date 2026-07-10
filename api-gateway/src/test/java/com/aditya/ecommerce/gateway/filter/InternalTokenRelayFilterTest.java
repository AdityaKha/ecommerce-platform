package com.aditya.ecommerce.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class InternalTokenRelayFilterTest {

    private static final String TOKEN = "unit-test-internal-token";

    private final InternalTokenRelayFilter filter = new InternalTokenRelayFilter(TOKEN);

    @Test
    void stampsInternalTokenOnForwardedRequest() {
        HttpHeaders forwarded = filterAndCaptureHeaders(
                MockServerHttpRequest.get("/api/products"));

        assertThat(forwarded.get("X-Internal-Token")).containsExactly(TOKEN);
    }

    @Test
    void stripsClientSuppliedIdentityAndTokenHeaders() {
        HttpHeaders forwarded = filterAndCaptureHeaders(
                MockServerHttpRequest.get("/api/auth/users")
                        .header("X-Auth-Subject", "attacker")
                        .header("X-Auth-Roles", "ROLE_ADMIN")
                        .header("X-Internal-Token", "guessed-token"));

        assertThat(forwarded.getFirst("X-Auth-Subject")).isNull();
        assertThat(forwarded.getFirst("X-Auth-Roles")).isNull();
        // The client's value is replaced, not appended to.
        assertThat(forwarded.get("X-Internal-Token")).containsExactly(TOKEN);
    }

    @Test
    void leavesUnrelatedHeadersIntact() {
        HttpHeaders forwarded = filterAndCaptureHeaders(
                MockServerHttpRequest.get("/api/products")
                        .header("Authorization", "Bearer some-jwt"));

        assertThat(forwarded.getFirst("Authorization")).isEqualTo("Bearer some-jwt");
    }

    private HttpHeaders filterAndCaptureHeaders(MockServerHttpRequest.BaseBuilder<?> requestBuilder) {
        MockServerWebExchange exchange = MockServerWebExchange.from(requestBuilder.build());
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            captured.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(captured.get()).isNotNull();
        return captured.get().getRequest().getHeaders();
    }
}
