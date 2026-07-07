package com.aditya.ecommerce.notification.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.mail.MailSendException;

import java.util.List;

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
    void mailProviderFailure_isSeekedForRetry() {
        ListenerExecutionFailedException transientFailure =
                new ListenerExecutionFailedException("listener failed", new MailSendException("smtp down"));

        // Spring Kafka signals "seek and redeliver" by throwing RecordInRetryException
        // (package-private, so we assert on its message).
        assertThatThrownBy(() -> handler.handleRemaining(transientFailure, List.of(record), consumer, container))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Record in retry");
    }
}
