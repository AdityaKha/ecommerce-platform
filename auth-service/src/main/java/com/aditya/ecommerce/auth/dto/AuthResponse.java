package com.aditya.ecommerce.auth.dto;

public record AuthResponse(
        String token,
        String username,
        long expiresInMillis
) {
}
