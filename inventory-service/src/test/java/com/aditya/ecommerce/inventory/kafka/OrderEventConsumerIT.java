package com.aditya.ecommerce.inventory.kafka;

import com.aditya.ecommerce.inventory.domain.InventoryItem;
import com.aditya.ecommerce.inventory.event.OrderCreatedEvent;
import com.aditya.ecommerce.inventory.repository.InventoryRepository;
import com.aditya.ecommerce.inventory.repository.ProcessedOrderEventRepository;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * auto-offset-reset is overridden to "earliest" for this test only: the consumer
 * group has no committed offset on a cold start, and with prod's "latest" default
 * there's a race where the test producer's send can land before the @KafkaListener
 * container finishes rebalancing, causing it to skip straight past the only message.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.kafka.consumer.auto-offset-reset=earliest")
@Testcontainers
class OrderEventConsumerIT {

    private static final String TOPIC = "order-events";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.7.0");

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProcessedOrderEventRepository processedOrderEventRepository;

    private KafkaProducer<String, OrderCreatedEvent> producer;

    @BeforeEach
    void seedInventory() {
        // Start from a clean slate: the V2 seed migration also inserts
        // product_id 1, which would violate the unique constraint here.
        processedOrderEventRepository.deleteAll();
        inventoryRepository.deleteAll();

        InventoryItem item = InventoryItem.builder()
                .productId(1L)
                .quantityAvailable(10)
                .build();
        inventoryRepository.save(item);

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        producer = new KafkaProducer<>(props);
    }

    @AfterEach
    void tearDown() {
        if (producer != null) {
            producer.close();
        }
    }

    @Test
    void realKafkaListener_reservesStockAndRecordsProcessedEvent() {
        long orderId = 9001L;
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                "jdoe",
                List.of(new OrderCreatedEvent.Item(1L, 3)),
                new BigDecimal("30.00"));

        producer.send(new ProducerRecord<>(TOPIC, String.valueOf(orderId), event));
        producer.flush();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<InventoryItem> updated = inventoryRepository.findByProductId(1L);
            assertThat(updated).isPresent();
            assertThat(updated.get().getQuantityAvailable()).isEqualTo(7);
            assertThat(processedOrderEventRepository.existsByOrderId(orderId)).isTrue();
        });
    }
}
