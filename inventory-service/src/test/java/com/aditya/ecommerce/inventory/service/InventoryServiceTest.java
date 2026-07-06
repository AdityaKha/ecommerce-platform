package com.aditya.ecommerce.inventory.service;

import com.aditya.ecommerce.inventory.domain.InventoryItem;
import com.aditya.ecommerce.inventory.domain.ProcessedOrderEvent;
import com.aditya.ecommerce.inventory.event.OrderCreatedEvent;
import com.aditya.ecommerce.inventory.exception.InsufficientStockException;
import com.aditya.ecommerce.inventory.repository.InventoryRepository;
import com.aditya.ecommerce.inventory.repository.ProcessedOrderEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProcessedOrderEventRepository processedOrderEventRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryItem itemWithStock(Long productId, int quantity) {
        return InventoryItem.builder()
                .id(1L)
                .productId(productId)
                .quantityAvailable(quantity)
                .build();
    }

    @Test
    void getByProductId_found_returnsItem() {
        InventoryItem item = itemWithStock(1L, 10);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(item));

        InventoryItem result = inventoryService.getByProductId(1L);

        assertThat(result).isSameAs(item);
    }

    @Test
    void getByProductId_notFound_throwsNoSuchElementException() {
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getByProductId(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void processOrderCreated_alreadyProcessed_skipsReservationEntirely() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                500L, "jdoe", List.of(new OrderCreatedEvent.Item(1L, 2)), new BigDecimal("20.00"));
        when(processedOrderEventRepository.existsByOrderId(500L)).thenReturn(true);

        inventoryService.processOrderCreated(event);

        verifyNoInteractions(inventoryRepository);
        verify(processedOrderEventRepository, never()).save(any());
    }

    @Test
    void processOrderCreated_newOrder_reservesStockForEveryItemAndSavesProcessedEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                600L,
                "jdoe",
                List.of(new OrderCreatedEvent.Item(1L, 2), new OrderCreatedEvent.Item(2L, 3)),
                new BigDecimal("50.00"));
        when(processedOrderEventRepository.existsByOrderId(600L)).thenReturn(false);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(itemWithStock(1L, 10)));
        when(inventoryRepository.findByProductId(2L)).thenReturn(Optional.of(itemWithStock(2L, 10)));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.processOrderCreated(event);

        ArgumentCaptor<InventoryItem> itemCaptor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryRepository, times(2)).save(itemCaptor.capture());
        List<InventoryItem> savedItems = itemCaptor.getAllValues();
        assertThat(savedItems).extracting(InventoryItem::getProductId).containsExactlyInAnyOrder(1L, 2L);
        assertThat(savedItems).extracting(InventoryItem::getQuantityAvailable).containsExactlyInAnyOrder(8, 7);

        ArgumentCaptor<ProcessedOrderEvent> processedCaptor = ArgumentCaptor.forClass(ProcessedOrderEvent.class);
        verify(processedOrderEventRepository).save(processedCaptor.capture());
        assertThat(processedCaptor.getValue().getOrderId()).isEqualTo(600L);
    }

    @Test
    void reserveStock_sufficientStock_decrementsAndSaves() {
        InventoryItem item = itemWithStock(1L, 10);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.reserveStock(1L, 4);

        ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantityAvailable()).isEqualTo(6);
    }

    @Test
    void reserveStock_insufficientStock_throwsAndDoesNotSave() {
        InventoryItem item = itemWithStock(1L, 2);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> inventoryService.reserveStock(1L, 5))
                .isInstanceOf(InsufficientStockException.class);

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void adjustStock_existingItem_addsDeltaAndSaves() {
        InventoryItem item = itemWithStock(1L, 10);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItem result = inventoryService.adjustStock(1L, 5);

        assertThat(result.getQuantityAvailable()).isEqualTo(15);
        verify(inventoryRepository).save(item);
    }

    @Test
    void adjustStock_missingProductId_createsNewItemStartingAtZeroBeforeApplyingDelta() {
        when(inventoryRepository.findByProductId(42L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItem result = inventoryService.adjustStock(42L, 7);

        assertThat(result.getProductId()).isEqualTo(42L);
        assertThat(result.getQuantityAvailable()).isEqualTo(7);
    }

    @Test
    void adjustStock_deltaMoreNegativeThanCurrentStock_clampsToZero() {
        InventoryItem item = itemWithStock(1L, 3);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItem result = inventoryService.adjustStock(1L, -10);

        assertThat(result.getQuantityAvailable()).isEqualTo(0);
    }
}
