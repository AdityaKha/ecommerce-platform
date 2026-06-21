package com.aditya.ecommerce.notification.kafka;

import com.aditya.ecommerce.notification.event.OrderCreatedEvent;
import com.aditya.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        notificationService.sendOrderConfirmation(event);
    }
}
