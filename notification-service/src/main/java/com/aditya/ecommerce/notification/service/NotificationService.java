package com.aditya.ecommerce.notification.service;

import com.aditya.ecommerce.notification.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    public void sendOrderConfirmation(OrderCreatedEvent event) {
        // TODO: integrate with an email/SMS/push provider.
        log.info("Sending order confirmation to {} for order {} (total: {})",
                event.customerUsername(), event.orderId(), event.totalAmount());
    }
}
