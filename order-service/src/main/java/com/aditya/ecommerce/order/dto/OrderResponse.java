package com.aditya.ecommerce.order.dto;

import com.aditya.ecommerce.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerUsername,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemRequest> items,
        Instant createdAt
) {
}
