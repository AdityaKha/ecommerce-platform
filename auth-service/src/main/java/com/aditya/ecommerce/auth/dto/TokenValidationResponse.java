package com.aditya.ecommerce.auth.dto;

import java.util.Set;

public record TokenValidationResponse(
        boolean valid,
        String username,
        Set<String> roles
) {
    public static TokenValidationResponse invalid() {
        return new TokenValidationResponse(false, null, Set.of());
    }
}
