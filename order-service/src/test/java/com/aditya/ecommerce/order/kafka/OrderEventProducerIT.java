package com.aditya.ecommerce.order.kafka;

import com.aditya.ecommerce.order.event.OrderCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class OrderEventProducerIT {

    // The full application context includes JPA/Flyway, so a real database is
    // needed even though this test only exercises the Kafka producer.
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.7.0");

    @Autowired
    private OrderEventProducer orderEventProducer;

    private KafkaConsumer<String, OrderCreatedEvent> consumer;

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void publishOrderCreated_sendsSerializedEventToRealBroker() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-events-test-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderCreatedEvent.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(OrderEventProducer.TOPIC));

        OrderCreatedEvent event = new OrderCreatedEvent(
                123L,
                "jdoe",
                "jdoe@example.com",
                List.of(new OrderCreatedEvent.Item(1L, 2)),
                new BigDecimal("36.50"));

        orderEventProducer.publishOrderCreated(event);

        ConsumerRecords<String, OrderCreatedEvent> records = poll(consumer, Duration.ofSeconds(15));
        assertThat(records.count()).isGreaterThanOrEqualTo(1);

        ConsumerRecord<String, OrderCreatedEvent> record = records.iterator().next();
        assertThat(record.key()).isEqualTo("123");

        OrderCreatedEvent received = record.value();
        assertThat(received.orderId()).isEqualTo(123L);
        assertThat(received.customerUsername()).isEqualTo("jdoe");
        assertThat(received.customerEmail()).isEqualTo("jdoe@example.com");
        assertThat(received.totalAmount()).isEqualByComparingTo("36.50");
        assertThat(received.items()).containsExactly(new OrderCreatedEvent.Item(1L, 2));
    }

    private static ConsumerRecords<String, OrderCreatedEvent> poll(
            KafkaConsumer<String, OrderCreatedEvent> consumer, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            ConsumerRecords<String, OrderCreatedEvent> records = consumer.poll(Duration.ofSeconds(1));
            if (!records.isEmpty()) {
                return records;
            }
        }
        return consumer.poll(Duration.ZERO);
    }
}
