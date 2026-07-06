package com.aditya.ecommerce.notification.strategy;

import com.aditya.ecommerce.notification.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SmsNotificationStrategyTest {

    private final SmsNotificationStrategy strategy = new SmsNotificationStrategy();

    @Test
    void reportsSmsAsItsChannel() {
        assertThat(strategy.channel()).isEqualTo(NotificationChannel.SMS);
    }

    @Test
    void sendDoesNotThrowForANormalEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                1L, "jdoe", "jdoe@example.com", List.of(new OrderCreatedEvent.Item(1L, 1)), BigDecimal.TEN);

        assertThatCode(() -> strategy.send(event)).doesNotThrowAnyException();
    }
}
