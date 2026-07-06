package com.aditya.ecommerce.gateway.filter;

import com.aditya.ecommerce.gateway.security.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "unit-test-only-secret-key-that-is-at-least-32-bytes-long";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);

    @Test
    void openPathsReachTheChainWithoutCheckingAuth() {
        MockServerWebExchange exchange = exchangeFor(
                MockServerHttpRequest.post("/api/auth/login"));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat(exchange.getRequest().getHeaders().getFirst("X-Auth-Subject")).isNull();
    }

    @Test
    void missingAuthorizationHeaderOnProtectedPathIsUnauthorized() {
        MockServerWebExchange exchange = exchangeFor(
                MockServerHttpRequest.get("/api/products"));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void malformedAuthorizationHeaderIsUnauthorized() {
        MockServerWebExchange exchange = exchangeFor(
                MockServerHttpRequest.get("/api/products")
                        .header("Authorization", "Token abc.def.ghi"));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void invalidTokenIsUnauthorized() {
        MockServerWebExchange exchange = exchangeFor(
                MockServerHttpRequest.get("/api/products")
                        .header("Authorization", "Bearer not-a-real-token"));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void validTokenReachesTheChainWithSubjectAndRolesForwarded() {
        String token = generateToken("jdoe", Set.of("ADMIN", "USER"));
        MockServerWebExchange exchange = exchangeFor(
                MockServerHttpRequest.get("/api/products")
                        .header("Authorization", "Bearer " + token));

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            captured.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerWebExchange mutated = captured.get();
        assertThat(mutated).isNotNull();
        assertThat(mutated.getRequest().getHeaders().getFirst("X-Auth-Subject")).isEqualTo("jdoe");

        String roles = mutated.getRequest().getHeaders().getFirst("X-Auth-Roles");
        assertThat(roles).isNotNull();
        assertThat(Set.of(roles.split(","))).isEqualTo(Set.of("ADMIN", "USER"));
    }

    private String generateToken(String subject, Set<String> roles) {
        SecretKey signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(signingKey)
                .compact();
    }

    private MockServerWebExchange exchangeFor(MockServerHttpRequest.BaseBuilder<?> requestBuilder) {
        return MockServerWebExchange.from(requestBuilder.build());
    }
}
