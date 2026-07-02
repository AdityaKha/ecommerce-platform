package com.aditya.ecommerce.order.service;

import com.aditya.ecommerce.order.client.InventoryClient;
import com.aditya.ecommerce.order.domain.Order;
import com.aditya.ecommerce.order.domain.OrderItem;
import com.aditya.ecommerce.order.domain.OrderStatus;
import com.aditya.ecommerce.order.dto.OrderItemRequest;
import com.aditya.ecommerce.order.dto.OrderRequest;
import com.aditya.ecommerce.order.dto.OrderResponse;
import com.aditya.ecommerce.order.event.OrderCreatedEvent;
import com.aditya.ecommerce.order.exception.InsufficientStockException;
import com.aditya.ecommerce.order.exception.InvalidOrderStatusTransitionException;
import com.aditya.ecommerce.order.kafka.OrderEventProducer;
import com.aditya.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.CREATED, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;
    private final InventoryClient inventoryClient;

    public OrderResponse createOrder(OrderRequest request) {
        request.items().forEach(item -> {
            if (!inventoryClient.hasSufficientStock(item.productId(), item.quantity())) {
                throw new InsufficientStockException(item.productId(), item.quantity());
            }
        });

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

    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));

        OrderStatus currentStatus = order.getStatus();
        if (!VALID_TRANSITIONS.get(currentStatus).contains(newStatus)) {
            throw new InvalidOrderStatusTransitionException(currentStatus, newStatus);
        }

        order.setStatus(newStatus);
        return toResponse(orderRepository.save(order));
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
