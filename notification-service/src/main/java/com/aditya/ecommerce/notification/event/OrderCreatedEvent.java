package com.aditya.ecommerce.notification.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mirrors the contract published by order-service on the "order-events" topic.
 * Kept as a local copy (no shared library) so this service deploys independently.
 */
public record OrderCreatedEvent(
        Long orderId,
        String customerUsername,
        String customerEmail,
        List<Item> items,
        BigDecimal totalAmount
) {
    public record Item(Long productId, Integer quantity) {
    }
}
