package com.aditya.ecommerce.inventory.security;

/**
 * Thrown when an authenticated caller lacks the role required for an operation.
 * Mapped to HTTP 403 by {@code GlobalExceptionHandler}.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
