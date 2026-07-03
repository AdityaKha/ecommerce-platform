package com.aditya.ecommerce.notification.strategy;

import com.aditya.ecommerce.notification.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Placeholder until a real SMS provider (e.g. Twilio) is wired in; kept behind
 * {@link NotificationStrategy} so swapping in a real implementation needs no
 * caller changes.
 */
@Slf4j
@Component
public class SmsNotificationStrategy implements NotificationStrategy {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(OrderCreatedEvent event) {
        log.info("[SMS stub] Order confirmation for {} (order {}, total {})",
                event.customerUsername(), event.orderId(), event.totalAmount());
    }
}
