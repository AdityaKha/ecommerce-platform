package com.aditya.ecommerce.order.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Event contract published to the "order-events" Kafka topic. inventory-service and
 * notification-service each keep their own copy of this record under their own
 * package (no shared library) so every service can evolve and deploy independently.
 * Consumers ignore unknown fields, so producer-side additions (e.g. customerEmail)
 * are safe to roll out without touching consumers that don't need them.
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
