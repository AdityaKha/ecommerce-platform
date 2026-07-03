package com.aditya.ecommerce.notification.service;

import com.aditya.ecommerce.notification.event.OrderCreatedEvent;
import com.aditya.ecommerce.notification.strategy.NotificationChannel;
import com.aditya.ecommerce.notification.strategy.NotificationStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final OrderCreatedEvent event = new OrderCreatedEvent(
            1L, "jdoe", "jdoe@example.com", List.of(new OrderCreatedEvent.Item(1L, 1)), BigDecimal.TEN);

    @Test
    void dispatchesToTheStrategyMatchingTheConfiguredChannel() {
        NotificationStrategy email = mock(NotificationStrategy.class);
        when(email.channel()).thenReturn(NotificationChannel.EMAIL);
        NotificationStrategy sms = mock(NotificationStrategy.class);
        when(sms.channel()).thenReturn(NotificationChannel.SMS);

        NotificationService service = new NotificationService(List.of(email, sms), NotificationChannel.EMAIL);
        service.sendOrderConfirmation(event);

        verify(email).send(event);
        verify(sms, never()).send(event);
    }

    @Test
    void switchingTheConfiguredChannelChangesWhichStrategyRuns() {
        NotificationStrategy email = mock(NotificationStrategy.class);
        when(email.channel()).thenReturn(NotificationChannel.EMAIL);
        NotificationStrategy sms = mock(NotificationStrategy.class);
        when(sms.channel()).thenReturn(NotificationChannel.SMS);

        NotificationService service = new NotificationService(List.of(email, sms), NotificationChannel.SMS);
        service.sendOrderConfirmation(event);

        verify(sms).send(event);
        verify(email, never()).send(event);
    }

    @Test
    void doesNothingWhenNoStrategyIsRegisteredForTheConfiguredChannel() {
        NotificationStrategy email = mock(NotificationStrategy.class);
        when(email.channel()).thenReturn(NotificationChannel.EMAIL);

        NotificationService service = new NotificationService(List.of(email), NotificationChannel.PUSH);
        service.sendOrderConfirmation(event);

        verify(email, never()).send(event);
    }
}
