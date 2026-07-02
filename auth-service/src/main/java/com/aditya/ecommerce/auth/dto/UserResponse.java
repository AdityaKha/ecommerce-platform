package com.aditya.ecommerce.auth.dto;

import com.aditya.ecommerce.auth.domain.Role;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        Set<Role> roles,
        Instant createdAt
) {
}
