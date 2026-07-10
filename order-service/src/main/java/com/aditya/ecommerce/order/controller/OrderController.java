package com.aditya.ecommerce.order.controller;

import com.aditya.ecommerce.order.dto.OrderRequest;
import com.aditya.ecommerce.order.dto.OrderResponse;
import com.aditya.ecommerce.order.dto.OrderStatusUpdateRequest;
import com.aditya.ecommerce.order.security.AccessControl;
import com.aditya.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // The order is always attributed to the authenticated caller (X-Auth-Subject),
    // never to whatever customerUsername the client put in the body — otherwise a
    // customer could place orders under someone else's identity.
    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody OrderRequest request,
            @RequestHeader(value = AccessControl.SUBJECT_HEADER, required = false) String subject) {
        String owner = AccessControl.requireSubject(subject);
        OrderRequest ownedRequest = new OrderRequest(owner, request.customerEmail(), request.items());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(ownedRequest));
    }

    @GetMapping("/{id}")
    public OrderResponse findById(
            @PathVariable Long id,
            @RequestHeader(value = AccessControl.SUBJECT_HEADER, required = false) String subject,
            @RequestHeader(value = AccessControl.ROLES_HEADER, required = false) String roles) {
        OrderResponse order = orderService.findById(id);
        AccessControl.requireOwnerOrAdmin(order.customerUsername(), subject, roles);
        return order;
    }

    // A customer sees only their own orders; an administrator sees all of them.
    @GetMapping
    public List<OrderResponse> findAll(
            @RequestHeader(value = AccessControl.SUBJECT_HEADER, required = false) String subject,
            @RequestHeader(value = AccessControl.ROLES_HEADER, required = false) String roles) {
        if (AccessControl.isAdmin(roles)) {
            return orderService.findAll();
        }
        return orderService.findByCustomer(AccessControl.requireSubject(subject));
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            @RequestHeader(value = AccessControl.SUBJECT_HEADER, required = false) String subject,
            @RequestHeader(value = AccessControl.ROLES_HEADER, required = false) String roles) {
        // Verify ownership before mutating anything.
        OrderResponse existing = orderService.findById(id);
        AccessControl.requireOwnerOrAdmin(existing.customerUsername(), subject, roles);
        return orderService.updateStatus(id, request.status());
    }
}
