package com.aditya.ecommerce.product.controller;

import com.aditya.ecommerce.product.dto.ProductRequest;
import com.aditya.ecommerce.product.dto.ProductResponse;
import com.aditya.ecommerce.product.security.AccessControl;
import com.aditya.ecommerce.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> findAll() {
        return productService.findAll();
    }

    @GetMapping("/search")
    public List<ProductResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return productService.search(name, category, minPrice, maxPrice);
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    // Catalog mutations are admin-only. GETs stay open to any authenticated
    // user; the gateway-verified role is read from the X-Auth-Roles header.
    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request,
            @RequestHeader(value = AccessControl.ROLES_HEADER, required = false) String roles) {
        AccessControl.requireAdmin(roles);
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            @RequestHeader(value = AccessControl.ROLES_HEADER, required = false) String roles) {
        AccessControl.requireAdmin(roles);
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = AccessControl.ROLES_HEADER, required = false) String roles) {
        AccessControl.requireAdmin(roles);
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
