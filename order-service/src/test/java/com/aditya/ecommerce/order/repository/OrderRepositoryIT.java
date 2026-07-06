package com.aditya.ecommerce.order.repository;

import com.aditya.ecommerce.order.domain.Order;
import com.aditya.ecommerce.order.domain.OrderItem;
import com.aditya.ecommerce.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void savedOrder_cascadesItemsAndCanBeReFetched() {
        OrderItem item1 = OrderItem.builder()
                .productId(1L)
                .quantity(2)
                .unitPrice(new BigDecimal("10.00"))
                .build();
        OrderItem item2 = OrderItem.builder()
                .productId(2L)
                .quantity(3)
                .unitPrice(new BigDecimal("5.50"))
                .build();

        Order order = Order.builder()
                .customerUsername("jdoe")
                .customerEmail("jdoe@example.com")
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("36.50"))
                .items(List.of(item1, item2))
                .build();

        item1.setOrder(order);
        item2.setOrder(order);

        Order saved = orderRepository.save(order);
        orderRepository.flush();

        Optional<Order> fetched = orderRepository.findById(saved.getId());

        assertThat(fetched).isPresent();
        Order fetchedOrder = fetched.get();
        assertThat(fetchedOrder.getCustomerUsername()).isEqualTo("jdoe");
        assertThat(fetchedOrder.getCustomerEmail()).isEqualTo("jdoe@example.com");
        assertThat(fetchedOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(fetchedOrder.getTotalAmount()).isEqualByComparingTo("36.50");
        assertThat(fetchedOrder.getCreatedAt()).isNotNull();

        assertThat(fetchedOrder.getItems()).hasSize(2);
        assertThat(fetchedOrder.getItems())
                .extracting(OrderItem::getProductId)
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(fetchedOrder.getItems())
                .allSatisfy(item -> assertThat(item.getOrder().getId()).isEqualTo(fetchedOrder.getId()));
    }
}
