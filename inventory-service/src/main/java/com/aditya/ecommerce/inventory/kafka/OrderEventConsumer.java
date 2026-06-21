package com.aditya.ecommerce.inventory.kafka;

import com.aditya.ecommerce.inventory.event.OrderCreatedEvent;
import com.aditya.ecommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Reserving stock for order {}", event.orderId());
        event.items().forEach(item -> inventoryService.reserveStock(item.productId(), item.quantity()));
    }
}
