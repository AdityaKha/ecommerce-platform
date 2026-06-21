package com.aditya.ecommerce.inventory.controller;

import com.aditya.ecommerce.inventory.domain.InventoryItem;
import com.aditya.ecommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public InventoryItem getByProductId(@PathVariable Long productId) {
        return inventoryService.getByProductId(productId);
    }

    @PutMapping("/{productId}/adjust")
    public InventoryItem adjust(@PathVariable Long productId, @RequestParam int delta) {
        return inventoryService.adjustStock(productId, delta);
    }
}
