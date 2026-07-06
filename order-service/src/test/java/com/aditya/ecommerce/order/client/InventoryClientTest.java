package com.aditya.ecommerce.order.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InventoryClientTest {

    private MockRestServiceServer mockServer;
    private InventoryClient inventoryClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        inventoryClient = new InventoryClient(builder);
    }

    @Test
    void hasSufficientStock_whenQuantityAvailableCoversRequest_returnsTrue() {
        mockServer.expect(requestTo("http://inventory-service/api/inventory/1"))
                .andRespond(withSuccess("{\"id\": 1, \"productId\": 1, \"quantityAvailable\": 10}", MediaType.APPLICATION_JSON));

        boolean result = inventoryClient.hasSufficientStock(1L, 5);

        assertThat(result).isTrue();
    }

    @Test
    void hasSufficientStock_whenRequestExceedsAvailable_returnsFalse() {
        mockServer.expect(requestTo("http://inventory-service/api/inventory/1"))
                .andRespond(withSuccess("{\"id\": 1, \"productId\": 1, \"quantityAvailable\": 10}", MediaType.APPLICATION_JSON));

        boolean result = inventoryClient.hasSufficientStock(1L, 20);

        assertThat(result).isFalse();
    }

    @Test
    void hasSufficientStock_whenProductNotFound_returnsFalse() {
        mockServer.expect(requestTo("http://inventory-service/api/inventory/1"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        boolean result = inventoryClient.hasSufficientStock(1L, 5);

        assertThat(result).isFalse();
    }

    @Test
    void hasSufficientStock_whenServerError_propagatesException() {
        mockServer.expect(requestTo("http://inventory-service/api/inventory/1"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> inventoryClient.hasSufficientStock(1L, 5))
                .isInstanceOf(RestClientResponseException.class);
    }
}
