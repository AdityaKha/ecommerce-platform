package com.aditya.ecommerce.notification.strategy;

import com.aditya.ecommerce.notification.event.OrderCreatedEvent;

public interface NotificationStrategy {

    NotificationChannel channel();

    void send(OrderCreatedEvent event);
}
