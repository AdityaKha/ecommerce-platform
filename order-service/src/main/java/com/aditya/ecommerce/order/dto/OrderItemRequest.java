package com.aditya.ecommerce.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotNull Long productId,
        @NotNull @Positive Integer quantity,
        @NotNull @Positive BigDecimal unitPrice
) {
}
