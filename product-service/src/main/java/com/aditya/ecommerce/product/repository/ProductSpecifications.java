package com.aditya.ecommerce.product.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.aditya.ecommerce.product.domain.Product;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> nameContains(String name) {
        return (root, query, cb) -> name == null || name.isBlank()
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> categoryEquals(String category) {
        return (root, query, cb) -> category == null || category.isBlank()
                ? null
                : cb.equal(root.get("category"), category);
    }

    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) -> minPrice == null ? null : cb.ge(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) -> maxPrice == null ? null : cb.le(root.get("price"), maxPrice);
    }
}
