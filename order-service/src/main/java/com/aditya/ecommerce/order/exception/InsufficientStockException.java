package com.aditya.ecommerce.order.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int requestedQuantity) {
        super("Insufficient stock for product " + productId + ": requested " + requestedQuantity);
    }
}
