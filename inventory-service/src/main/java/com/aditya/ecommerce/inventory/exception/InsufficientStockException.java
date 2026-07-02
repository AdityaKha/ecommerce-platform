package com.aditya.ecommerce.inventory.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int requestedQuantity, int available) {
        super("Insufficient stock for product " + productId + ": requested " + requestedQuantity
                + ", available " + available);
    }
}
