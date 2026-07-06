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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private OrderService orderService;

    private static OrderRequest requestWithItems(OrderItemRequest... items) {
        return new OrderRequest("jdoe", "jdoe@example.com", List.of(items));
    }

    private static Order savedOrderFrom(Order order, long id) {
        // Simulate JPA assigning an id and @PrePersist setting createdAt, keeping items/order back-refs intact.
        order.setId(id);
        order.setCreatedAt(Instant.now());
        return order;
    }

    @Test
    void createOrder_happyPath_computesTotalAndPublishesEvent() {
        OrderItemRequest item1 = new OrderItemRequest(1L, 2, new BigDecimal("10.00"));
        OrderItemRequest item2 = new OrderItemRequest(2L, 3, new BigDecimal("5.50"));
        OrderRequest request = requestWithItems(item1, item2);

        when(inventoryClient.hasSufficientStock(anyLong(), anyInt())).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            return savedOrderFrom(order, 42L);
        });

        OrderResponse response = orderService.createOrder(request);

        // total = 2 * 10.00 + 3 * 5.50 = 20.00 + 16.50 = 36.50
        assertThat(response.totalAmount()).isEqualByComparingTo("36.50");
        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.customerUsername()).isEqualTo("jdoe");
        assertThat(response.customerEmail()).isEqualTo("jdoe@example.com");
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.items()).hasSize(2);

        verify(inventoryClient).hasSufficientStock(1L, 2);
        verify(inventoryClient).hasSufficientStock(2L, 3);
        verify(orderRepository).save(any(Order.class));

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventProducer).publishOrderCreated(eventCaptor.capture());
        OrderCreatedEvent publishedEvent = eventCaptor.getValue();

        assertThat(publishedEvent.orderId()).isEqualTo(42L);
        assertThat(publishedEvent.customerUsername()).isEqualTo("jdoe");
        assertThat(publishedEvent.customerEmail()).isEqualTo("jdoe@example.com");
        assertThat(publishedEvent.totalAmount()).isEqualByComparingTo("36.50");
        assertThat(publishedEvent.items())
                .containsExactlyInAnyOrder(
                        new OrderCreatedEvent.Item(1L, 2),
                        new OrderCreatedEvent.Item(2L, 3));
    }

    @Test
    void createOrder_insufficientStock_throwsAndNeverSavesOrPublishes() {
        OrderItemRequest item = new OrderItemRequest(1L, 100, new BigDecimal("10.00"));
        OrderRequest request = requestWithItems(item);

        when(inventoryClient.hasSufficientStock(1L, 100)).thenReturn(false);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderEventProducer);
    }

    @Test
    void findById_found_returnsMappedResponse() {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .productId(5L)
                .quantity(2)
                .unitPrice(new BigDecimal("9.99"))
                .build();
        Order order = Order.builder()
                .id(7L)
                .customerUsername("alice")
                .customerEmail("alice@example.com")
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("19.98"))
                .items(List.of(item))
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.findById(7L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.customerUsername()).isEqualTo("alice");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productId()).isEqualTo(5L);
    }

    @Test
    void findById_notFound_throwsNoSuchElementException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void findAll_mapsAllOrders() {
        Order order1 = Order.builder()
                .id(1L)
                .customerUsername("bob")
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.TEN)
                .items(List.of())
                .createdAt(Instant.now())
                .build();
        Order order2 = Order.builder()
                .id(2L)
                .customerUsername("carol")
                .status(OrderStatus.CONFIRMED)
                .totalAmount(BigDecimal.ONE)
                .items(List.of())
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

        List<OrderResponse> responses = orderService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(OrderResponse::id).containsExactly(1L, 2L);
    }

    @ParameterizedTest
    @CsvSource({
            "CREATED, CONFIRMED",
            "CREATED, CANCELLED",
            "CONFIRMED, SHIPPED",
            "CONFIRMED, CANCELLED",
            "SHIPPED, DELIVERED"
    })
    void updateStatus_validTransition_succeeds(OrderStatus from, OrderStatus to) {
        Order order = Order.builder()
                .id(1L)
                .customerUsername("dave")
                .status(from)
                .totalAmount(BigDecimal.TEN)
                .items(List.of())
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateStatus(1L, to);

        assertThat(response.status()).isEqualTo(to);
        verify(orderRepository).save(order);
    }

    @ParameterizedTest
    @CsvSource({
            "CREATED, DELIVERED",
            "CREATED, SHIPPED"
    })
    void updateStatus_invalidTransition_throws(OrderStatus from, OrderStatus to) {
        Order order = Order.builder()
                .id(1L)
                .customerUsername("dave")
                .status(from)
                .totalAmount(BigDecimal.TEN)
                .items(List.of())
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L, to))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);

        verify(orderRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"DELIVERED", "CANCELLED"})
    void updateStatus_fromTerminalState_alwaysThrows(OrderStatus terminalStatus) {
        Order order = Order.builder()
                .id(1L)
                .customerUsername("dave")
                .status(terminalStatus)
                .totalAmount(BigDecimal.TEN)
                .items(List.of())
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.CONFIRMED))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);

        verify(orderRepository, never()).save(any());
    }
}
