package com.aditya.ecommerce.order.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Load-balanced client resolving "inventory-service" via Eureka. Used to check stock
 * availability synchronously before an order is created, in addition to the
 * defense-in-depth stock guard inventory-service applies on the Kafka event it receives.
 */
@Slf4j
@Component
public class InventoryClient {

    private static final String INVENTORY_SERVICE_URL = "http://inventory-service";

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder loadBalancedRestClientBuilder) {
        this.restClient = loadBalancedRestClientBuilder.baseUrl(INVENTORY_SERVICE_URL).build();
    }

    public boolean hasSufficientStock(Long productId, int quantity) {
        try {
            InventoryItemResponse item = restClient.get()
                    .uri("/api/inventory/{productId}", productId)
                    .retrieve()
                    .body(InventoryItemResponse.class);
            return item != null && item.quantityAvailable() != null && item.quantityAvailable() >= quantity;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return false;
            }
            log.warn("Inventory availability check failed for product {}: {}", productId, e.getMessage());
            throw e;
        }
    }
}
