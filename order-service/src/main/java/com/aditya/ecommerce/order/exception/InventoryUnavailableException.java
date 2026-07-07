package com.aditya.ecommerce.order.exception;

/**
 * Raised when the inventory-service cannot be reached (or its circuit breaker
 * is open), so stock availability cannot be verified. Mapped to 503 so the
 * client knows to retry rather than treat the order as rejected.
 */
public class InventoryUnavailableException extends RuntimeException {

    public InventoryUnavailableException(Long productId, Throwable cause) {
        super("Unable to verify stock for product " + productId
                + ": inventory service is currently unavailable", cause);
    }
}
