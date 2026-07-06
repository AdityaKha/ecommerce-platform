package com.aditya.ecommerce.inventory.repository;

import com.aditya.ecommerce.inventory.domain.ProcessedOrderEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ProcessedOrderEventRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ProcessedOrderEventRepository processedOrderEventRepository;

    @Test
    void existsByOrderId_afterSave_returnsTrue() {
        ProcessedOrderEvent event = ProcessedOrderEvent.builder()
                .orderId(201L)
                .processedAt(Instant.now())
                .build();

        processedOrderEventRepository.save(event);

        assertThat(processedOrderEventRepository.existsByOrderId(201L)).isTrue();
    }

    @Test
    void existsByOrderId_unknownOrderId_returnsFalse() {
        assertThat(processedOrderEventRepository.existsByOrderId(999999L)).isFalse();
    }
}
