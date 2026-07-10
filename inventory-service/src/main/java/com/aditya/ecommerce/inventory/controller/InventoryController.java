package com.aditya.ecommerce.inventory.controller;

import com.aditya.ecommerce.inventory.domain.InventoryItem;
import com.aditya.ecommerce.inventory.security.AccessControl;
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

    // Stock adjustment is admin-only. Reads stay open to any authenticated user
    // (order-service's availability check needs GET); the gateway-verified role
    // is read from the X-Auth-Roles header.
    @PutMapping("/{productId}/adjust")
    public InventoryItem adjust(
            @PathVariable Long productId,
            @RequestParam int delta,
            @RequestHeader(value = AccessControl.ROLES_HEADER, required = false) String roles) {
        AccessControl.requireAdmin(roles);
        return inventoryService.adjustStock(productId, delta);
    }
}
