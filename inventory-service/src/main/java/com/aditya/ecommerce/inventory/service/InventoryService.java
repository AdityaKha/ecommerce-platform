package com.aditya.ecommerce.inventory.service;

import com.aditya.ecommerce.inventory.domain.InventoryItem;
import com.aditya.ecommerce.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryItem getByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("No inventory record for product: " + productId));
    }

    public void reserveStock(Long productId, int quantity) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("No inventory record for product: " + productId));

        int remaining = item.getQuantityAvailable() - quantity;
        if (remaining < 0) {
            log.warn("Insufficient stock for product {}: requested {}, available {}",
                    productId, quantity, item.getQuantityAvailable());
            remaining = 0;
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
