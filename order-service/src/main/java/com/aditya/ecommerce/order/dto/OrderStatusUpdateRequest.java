package com.aditya.ecommerce.order.dto;

import com.aditya.ecommerce.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull OrderStatus status
) {
}
