package com.aditya.ecommerce.product.repository;

import com.aditya.ecommerce.product.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.jpa.domain.Specification;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ProductRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        productRepository.save(Product.builder()
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .sku("SKU-MOUSE-1")
                .price(new BigDecimal("25.00"))
                .category("Electronics")
                .active(true)
                .build());

        productRepository.save(Product.builder()
                .name("Mechanical Keyboard")
                .description("RGB mechanical keyboard")
                .sku("SKU-KEYBOARD-1")
                .price(new BigDecimal("75.00"))
                .category("Electronics")
                .active(true)
                .build());

        productRepository.save(Product.builder()
                .name("Office Chair")
                .description("Adjustable office chair")
                .sku("SKU-CHAIR-1")
                .price(new BigDecimal("150.00"))
                .category("Furniture")
                .active(true)
                .build());

        productRepository.save(Product.builder()
                .name("Standing Desk")
                .description("Height-adjustable standing desk")
                .sku("SKU-DESK-1")
                .price(new BigDecimal("300.00"))
                .category("Furniture")
                .active(false)
                .build());

        productRepository.save(Product.builder()
                .name("USB-C Hub")
                .description("Multiport USB-C hub")
                .sku("SKU-HUB-1")
                .price(new BigDecimal("40.00"))
                .category("Electronics")
                .active(true)
                .build());
    }

    @Test
    void findAll_withNoFilters_returnsEverything() {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.nameContains(null),
                ProductSpecifications.categoryEquals(null),
                ProductSpecifications.priceGreaterThanOrEqual(null),
                ProductSpecifications.priceLessThanOrEqual(null));

        List<Product> results = productRepository.findAll(spec);

        assertThat(results).hasSize(5);
    }

    @Test
    void nameContains_isCaseInsensitive() {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.nameContains("keyboard"),
                ProductSpecifications.categoryEquals(null),
                ProductSpecifications.priceGreaterThanOrEqual(null),
                ProductSpecifications.priceLessThanOrEqual(null));

        List<Product> results = productRepository.findAll(spec);

        assertThat(results).extracting(Product::getSku).containsExactly("SKU-KEYBOARD-1");
    }

    @Test
    void categoryEquals_matchesExactCategory() {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.nameContains(null),
                ProductSpecifications.categoryEquals("Furniture"),
                ProductSpecifications.priceGreaterThanOrEqual(null),
                ProductSpecifications.priceLessThanOrEqual(null));

        List<Product> results = productRepository.findAll(spec);

        assertThat(results).extracting(Product::getSku)
                .containsExactlyInAnyOrder("SKU-CHAIR-1", "SKU-DESK-1");
    }

    @Test
    void priceRange_filtersBetweenMinAndMax() {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.nameContains(null),
                ProductSpecifications.categoryEquals(null),
                ProductSpecifications.priceGreaterThanOrEqual(new BigDecimal("30.00")),
                ProductSpecifications.priceLessThanOrEqual(new BigDecimal("150.00")));

        List<Product> results = productRepository.findAll(spec);

        assertThat(results).extracting(Product::getSku)
                .containsExactlyInAnyOrder("SKU-KEYBOARD-1", "SKU-CHAIR-1", "SKU-HUB-1");
    }

    @Test
    void combinedFilters_narrowDownResults() {
        // "USB-C Hub" contains no letter "e", so despite matching category/price it's
        // correctly excluded by nameContains("e") - only "Wireless Mouse" matches all four.
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.nameContains("e"),
                ProductSpecifications.categoryEquals("Electronics"),
                ProductSpecifications.priceGreaterThanOrEqual(new BigDecimal("20.00")),
                ProductSpecifications.priceLessThanOrEqual(new BigDecimal("50.00")));

        List<Product> results = productRepository.findAll(spec);

        assertThat(results).extracting(Product::getSku)
                .containsExactlyInAnyOrder("SKU-MOUSE-1");
    }
}
