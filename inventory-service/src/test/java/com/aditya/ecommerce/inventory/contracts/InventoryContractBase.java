package com.aditya.ecommerce.inventory.contracts;

import com.aditya.ecommerce.inventory.controller.InventoryController;
import com.aditya.ecommerce.inventory.domain.InventoryItem;
import com.aditya.ecommerce.inventory.exception.GlobalExceptionHandler;
import com.aditya.ecommerce.inventory.service.InventoryService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.NoSuchElementException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Base class for the contract verification tests the Spring Cloud Contract
 * maven plugin generates from src/test/resources/contracts. Serves the real
 * controller + exception handler with a mocked service layer, so the tests
 * verify the HTTP contract without a database.
 */
public abstract class InventoryContractBase {

    @BeforeEach
    void setUp() {
        InventoryService inventoryService = mock(InventoryService.class);

        when(inventoryService.getByProductId(1L)).thenReturn(InventoryItem.builder()
                .id(1L)
                .productId(1L)
                .quantityAvailable(25)
                .build());
        when(inventoryService.getByProductId(999L))
                .thenThrow(new NoSuchElementException("No inventory record for product: 999"));

        RestAssuredMockMvc.standaloneSetup(MockMvcBuilders
                .standaloneSetup(new InventoryController(inventoryService))
                .setControllerAdvice(new GlobalExceptionHandler()));
    }
}
