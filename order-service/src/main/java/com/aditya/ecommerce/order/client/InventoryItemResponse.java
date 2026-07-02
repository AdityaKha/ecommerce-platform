package com.aditya.ecommerce.order.client;

public record InventoryItemResponse(
        Long id,
        Long productId,
        Integer quantityAvailable
) {
}
