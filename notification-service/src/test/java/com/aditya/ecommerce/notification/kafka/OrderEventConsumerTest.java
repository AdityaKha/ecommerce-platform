package com.aditya.ecommerce.notification.kafka;

import com.aditya.ecommerce.notification.event.OrderCreatedEvent;
import com.aditya.ecommerce.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderEventConsumer consumer;

    @Test
    void delegatesStraightToNotificationService() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                1L, "jdoe", "jdoe@example.com", List.of(new OrderCreatedEvent.Item(1L, 1)), BigDecimal.TEN);

        consumer.onOrderCreated(event);

        verify(notificationService).sendOrderConfirmation(event);
        verifyNoMoreInteractions(notificationService);
    }
}
