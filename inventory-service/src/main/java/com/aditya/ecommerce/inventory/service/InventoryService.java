package com.aditya.ecommerce.inventory.service;

import com.aditya.ecommerce.inventory.domain.InventoryItem;
import com.aditya.ecommerce.inventory.domain.ProcessedOrderEvent;
import com.aditya.ecommerce.inventory.event.OrderCreatedEvent;
import com.aditya.ecommerce.inventory.exception.InsufficientStockException;
import com.aditya.ecommerce.inventory.repository.InventoryRepository;
import com.aditya.ecommerce.inventory.repository.ProcessedOrderEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProcessedOrderEventRepository processedOrderEventRepository;

    public InventoryItem getByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("No inventory record for product: " + productId));
    }

    @Transactional
    public void processOrderCreated(OrderCreatedEvent event) {
        if (processedOrderEventRepository.existsByOrderId(event.orderId())) {
            log.info("Order {} already processed, skipping duplicate delivery", event.orderId());
            return;
        }

        event.items().forEach(item -> reserveStock(item.productId(), item.quantity()));

        processedOrderEventRepository.save(ProcessedOrderEvent.builder()
                .orderId(event.orderId())
                .processedAt(Instant.now())
                .build());
    }

    public void reserveStock(Long productId, int quantity) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("No inventory record for product: " + productId));

        int remaining = item.getQuantityAvailable() - quantity;
        if (remaining < 0) {
            throw new InsufficientStockException(productId, quantity, item.getQuantityAvailable());
        }

        item.setQuantityAvailable(remaining);
        inventoryRepository.save(item);
    }

    public InventoryItem adjustStock(Long productId, int delta) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> InventoryItem.builder().productId(productId).quantityAvailable(0).build());

        item.setQuantityAvailable(Math.max(0, item.getQuantityAvailable() + delta));
        return inventoryRepository.save(item);
    }
}
