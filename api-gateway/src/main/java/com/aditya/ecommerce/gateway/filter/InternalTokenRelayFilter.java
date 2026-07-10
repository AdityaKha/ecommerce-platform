package com.aditya.ecommerce.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service-to-service auth, gateway side. Every proxied request is stamped with
 * the shared X-Internal-Token header, which downstream services require before
 * serving anything but actuator probes — so a request that bypasses the
 * gateway and hits a service port directly is rejected.
 *
 * Just as importantly, the identity headers the platform trusts internally
 * (X-Auth-Subject / X-Auth-Roles, set by {@link JwtAuthenticationFilter} after
 * signature verification) and the internal token itself are stripped from the
 * incoming request first, so a client can never smuggle its own values past
 * the gateway — e.g. sending X-Auth-Roles: ROLE_ADMIN alongside a valid
 * customer JWT, or forging identity on the unauthenticated auth routes.
 *
 * Runs before {@link JwtAuthenticationFilter} (order -1) so the verified
 * headers it sets are not wiped by the stripping here.
 */
@Component
public class InternalTokenRelayFilter implements GlobalFilter, Ordered {

    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private static final List<String> RESERVED_HEADERS =
            List.of("X-Auth-Subject", "X-Auth-Roles", INTERNAL_TOKEN_HEADER);

    private final String internalToken;

    public InternalTokenRelayFilter(@Value("${internal.token}") String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    RESERVED_HEADERS.forEach(headers::remove);
                    headers.set(INTERNAL_TOKEN_HEADER, internalToken);
                })
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -3;
    }
}
