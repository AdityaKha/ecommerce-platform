package com.aditya.ecommerce.order.security;

/**
 * Thrown when an authenticated caller tries to access or mutate an order they
 * do not own (and is not an administrator). Mapped to HTTP 403 by
 * {@code GlobalExceptionHandler}.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
