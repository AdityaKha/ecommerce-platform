package com.aditya.ecommerce.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Trusts the identity api-gateway already established: it verified the JWT signature
 * and forwards the subject/roles as headers. This service never re-parses the token —
 * only the gateway is reachable from outside the cluster, so these headers are safe
 * to trust coming from it.
 */
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String SUBJECT_HEADER = "X-Auth-Subject";
    private static final String ROLES_HEADER = "X-Auth-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String subject = request.getHeader(SUBJECT_HEADER);
        if (subject != null && !subject.isBlank()) {
            List<GrantedAuthority> authorities = parseRoles(request.getHeader(ROLES_HEADER));
            var authentication = new UsernamePasswordAuthenticationToken(subject, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private List<GrantedAuthority> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return List.of();
        }
        return List.of(rolesHeader.split(",")).stream()
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
    }
}
