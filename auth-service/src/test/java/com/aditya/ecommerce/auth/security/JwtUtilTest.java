package com.aditya.ecommerce.auth.security;

import com.aditya.ecommerce.auth.domain.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "a-test-secret-key-that-is-at-least-32-bytes-long";

    @Test
    void generateToken_thenIsValidAndExtractClaims_returnsSubjectAndRoles() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3600000L);

        String token = jwtUtil.generateToken("jdoe", Set.of(Role.ROLE_CUSTOMER, Role.ROLE_ADMIN));

        assertThat(jwtUtil.isValid(token)).isTrue();

        Claims claims = jwtUtil.extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo("jdoe");
        List<String> roles = claims.get("roles", List.class);
        assertThat(roles).containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN");
    }

    @Test
    void isValid_tokenSignedWithDifferentSecret_returnsFalse() {
        JwtUtil signer = new JwtUtil(SECRET, 3600000L);
        JwtUtil verifier = new JwtUtil("a-different-test-secret-key-that-is-at-least-32-bytes", 3600000L);

        String token = signer.generateToken("jdoe", Set.of(Role.ROLE_CUSTOMER));

        assertThat(verifier.isValid(token)).isFalse();
    }

    @Test
    void isValid_expiredToken_returnsFalse() throws InterruptedException {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 1L);

        String token = jwtUtil.generateToken("jdoe", Set.of(Role.ROLE_CUSTOMER));
        Thread.sleep(5);

        assertThat(jwtUtil.isValid(token)).isFalse();
    }

    @Test
    void isValid_garbageString_returnsFalseWithoutThrowing() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3600000L);

        assertThat(jwtUtil.isValid("not-a-real-jwt-token")).isFalse();
    }
}
