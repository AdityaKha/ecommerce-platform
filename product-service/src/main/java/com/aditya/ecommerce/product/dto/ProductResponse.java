package com.aditya.ecommerce.product.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String sku,
        BigDecimal price,
        String category,
        boolean active
) {
}
