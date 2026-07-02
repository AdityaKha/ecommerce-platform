package com.aditya.ecommerce.inventory.repository;

import com.aditya.ecommerce.inventory.domain.ProcessedOrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedOrderEventRepository extends JpaRepository<ProcessedOrderEvent, Long> {
    boolean existsByOrderId(Long orderId);
}
