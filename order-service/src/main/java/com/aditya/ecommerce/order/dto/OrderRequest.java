package com.aditya.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequest(
        @NotBlank String customerUsername,
        @Email String customerEmail,
        @NotEmpty @Valid List<OrderItemRequest> items
) {
}
