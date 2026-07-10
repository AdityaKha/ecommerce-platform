package com.aditya.ecommerce.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Service-to-service auth, service side. Only api-gateway (and trusted sibling
 * services) know the shared INTERNAL_TOKEN and stamp it on requests as
 * X-Internal-Token; anything arriving without it — i.e. a caller that bypassed
 * the gateway and hit this service's port directly — is rejected with 401.
 * This is what makes it safe for the service to trust the gateway-supplied
 * X-Auth-Subject / X-Auth-Roles identity headers.
 *
 * Actuator endpoints stay open for infrastructure health probes (only
 * health/info are exposed). Ordered ahead of the Spring Security filter chain
 * so unauthenticated direct calls are rejected before any other processing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final byte[] expectedToken;

    public InternalTokenFilter(@Value("${internal.token}") String internalToken) {
        this.expectedToken = internalToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String provided = request.getHeader(INTERNAL_TOKEN_HEADER);
        // Constant-time comparison so the token can't be guessed byte-by-byte
        // via response timing.
        if (provided == null
                || !MessageDigest.isEqual(expectedToken, provided.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Requests must come through the API gateway\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
