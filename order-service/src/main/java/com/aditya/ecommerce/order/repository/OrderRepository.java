package com.aditya.ecommerce.order.repository;

import com.aditya.ecommerce.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerUsername(String customerUsername);
}
