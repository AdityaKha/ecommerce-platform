package com.aditya.ecommerce.inventory.repository;

import com.aditya.ecommerce.inventory.domain.InventoryItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class InventoryRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void savedItem_canBeFoundByProductId() {
        InventoryItem item = InventoryItem.builder()
                .productId(101L)
                .quantityAvailable(25)
                .build();

        inventoryRepository.save(item);

        Optional<InventoryItem> found = inventoryRepository.findByProductId(101L);

        assertThat(found).isPresent();
        assertThat(found.get().getProductId()).isEqualTo(101L);
        assertThat(found.get().getQuantityAvailable()).isEqualTo(25);
    }

    @Test
    void findByProductId_unknownId_returnsEmpty() {
        Optional<InventoryItem> found = inventoryRepository.findByProductId(999999L);

        assertThat(found).isEmpty();
    }
}
