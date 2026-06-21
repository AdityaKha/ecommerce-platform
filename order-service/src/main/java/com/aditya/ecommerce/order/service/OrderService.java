package com.aditya.ecommerce.order.service;

import com.aditya.ecommerce.order.domain.Order;
import com.aditya.ecommerce.order.domain.OrderItem;
import com.aditya.ecommerce.order.domain.OrderStatus;
import com.aditya.ecommerce.order.dto.OrderItemRequest;
import com.aditya.ecommerce.order.dto.OrderRequest;
import com.aditya.ecommerce.order.dto.OrderResponse;
import com.aditya.ecommerce.order.event.OrderCreatedEvent;
import com.aditya.ecommerce.order.kafka.OrderEventProducer;
import com.aditya.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    public OrderResponse createOrder(OrderRequest request) {
        List<OrderItem> items = request.items().stream()
                .map(item -> OrderItem.builder()
                        .productId(item.productId())
                        .quantity(item.quantity())
                        .unitPrice(item.unitPrice())
                        .build())
                .toList();

        BigDecimal total = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .customerUsername(request.customerUsername())
                .status(OrderStatus.CREATED)
                .totalAmount(total)
                .items(items)
                .build();

        items.forEach(item -> item.setOrder(order));

        Order saved = orderRepository.save(order);

        orderEventProducer.publishOrderCreated(new OrderCreatedEvent(
                saved.getId(),
                saved.getCustomerUsername(),
                saved.getItems().stream()
                        .map(i -> new OrderCreatedEvent.Item(i.getProductId(), i.getQuantity()))
                        .toList(),
                saved.getTotalAmount()
        ));

        return toResponse(saved);
    }

    public OrderResponse findById(Long id) {
        return toResponse(orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id)));
    }

    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemRequest> items = order.getItems().stream()
                .map(i -> new OrderItemRequest(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerUsername(),
                order.getStatus(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt()
        );
    }
}
