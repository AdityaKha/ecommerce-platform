package com.aditya.ecommerce.order.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Event contract published to the "order-events" Kafka topic. inventory-service and
 * notification-service each keep an identical copy of this record under their own
 * package (no shared library) so every service can evolve and deploy independently.
 */
public record OrderCreatedEvent(
        Long orderId,
        String customerUsername,
        List<Item> items,
        BigDecimal totalAmount
) {
    public record Item(Long productId, Integer quantity) {
    }
}
