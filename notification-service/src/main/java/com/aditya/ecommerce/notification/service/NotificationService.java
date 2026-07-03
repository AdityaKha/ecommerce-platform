package com.aditya.ecommerce.notification.service;

import com.aditya.ecommerce.notification.event.OrderCreatedEvent;
import com.aditya.ecommerce.notification.strategy.NotificationChannel;
import com.aditya.ecommerce.notification.strategy.NotificationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationService {

    private final Map<NotificationChannel, NotificationStrategy> strategiesByChannel;
    private final NotificationChannel activeChannel;

    public NotificationService(List<NotificationStrategy> strategies,
                                @Value("${notification.channel:EMAIL}") NotificationChannel activeChannel) {
        this.strategiesByChannel = strategies.stream()
                .collect(Collectors.toMap(NotificationStrategy::channel, Function.identity()));
        this.activeChannel = activeChannel;
    }

    public void sendOrderConfirmation(OrderCreatedEvent event) {
        NotificationStrategy strategy = strategiesByChannel.get(activeChannel);
        if (strategy == null) {
            log.warn("No NotificationStrategy registered for channel {}; dropping notification for order {}",
                    activeChannel, event.orderId());
            return;
        }
        strategy.send(event);
    }
}
