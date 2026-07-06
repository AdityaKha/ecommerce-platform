package com.aditya.ecommerce.inventory.kafka;

import com.aditya.ecommerce.inventory.event.OrderCreatedEvent;
import com.aditya.ecommerce.inventory.exception.InsufficientStockException;
import com.aditya.ecommerce.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    private OrderCreatedEvent sampleEvent() {
        return new OrderCreatedEvent(
                700L, "jdoe", List.of(new OrderCreatedEvent.Item(1L, 2)), new BigDecimal("20.00"));
    }

    @Test
    void onOrderCreated_delegatesToInventoryService() {
        OrderCreatedEvent event = sampleEvent();

        orderEventConsumer.onOrderCreated(event);

        verify(inventoryService).processOrderCreated(event);
    }

    @Test
    void onOrderCreated_insufficientStockException_doesNotPropagate() {
        OrderCreatedEvent event = sampleEvent();
        doThrow(new InsufficientStockException(1L, 2, 0))
                .when(inventoryService).processOrderCreated(event);

        assertThatCode(() -> orderEventConsumer.onOrderCreated(event)).doesNotThrowAnyException();
    }
}
