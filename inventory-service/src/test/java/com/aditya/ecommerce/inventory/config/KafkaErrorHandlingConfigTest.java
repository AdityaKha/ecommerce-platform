package com.aditya.ecommerce.inventory.config;

import com.aditya.ecommerce.inventory.exception.InsufficientStockException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class KafkaErrorHandlingConfigTest {

    private final DefaultErrorHandler handler = new KafkaErrorHandlingConfig().kafkaErrorHandler();

    private final ConsumerRecord<String, String> record =
            new ConsumerRecord<>("order-events", 0, 0L, "key", "value");

    @SuppressWarnings("unchecked")
    private final Consumer<String, String> consumer = mock(Consumer.class);
    private final MessageListenerContainer container = mock(MessageListenerContainer.class);

    @Test
    void transientFailure_isSeekedForRetry() {
        ListenerExecutionFailedException transientFailure =
                new ListenerExecutionFailedException("listener failed", new RuntimeException("db down"));

        // Spring Kafka signals "seek and redeliver" by throwing RecordInRetryException
        // (package-private, so we assert on its message).
        assertThatThrownBy(() -> handler.handleRemaining(transientFailure, List.of(record), consumer, container))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Record in retry");
    }

    @Test
    void insufficientStock_isDroppedWithoutRetry() {
        ListenerExecutionFailedException businessFailure = new ListenerExecutionFailedException(
                "listener failed", new InsufficientStockException(1L, 5, 2));

        assertThatCode(() -> handler.handleRemaining(businessFailure, List.of(record), consumer, container))
                .doesNotThrowAnyException();
    }

    @Test
    void missingInventoryRecord_isDroppedWithoutRetry() {
        ListenerExecutionFailedException businessFailure = new ListenerExecutionFailedException(
                "listener failed", new NoSuchElementException("No inventory record for product: 1"));

        assertThatCode(() -> handler.handleRemaining(businessFailure, List.of(record), consumer, container))
                .doesNotThrowAnyException();
    }
}
