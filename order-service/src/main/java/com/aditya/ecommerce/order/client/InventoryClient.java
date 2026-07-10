package com.aditya.ecommerce.order.client;

import com.aditya.ecommerce.order.exception.InventoryUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Load-balanced client resolving "inventory-service" via Eureka. Used to check stock
 * availability synchronously before an order is created, in addition to the
 * defense-in-depth stock guard inventory-service applies on the Kafka event it receives.
 *
 * Calls are wrapped in a Resilience4j circuit breaker ("inventory"): repeated
 * failures open the circuit so order-service fails fast with a 503 instead of
 * piling up threads on a struggling inventory-service.
 */
@Slf4j
@Component
public class InventoryClient {

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public InventoryClient(RestClient.Builder loadBalancedRestClientBuilder,
                           CircuitBreakerFactory<?, ?> circuitBreakerFactory,
                           @Value("${inventory.service.url:http://inventory-service}") String inventoryServiceUrl,
                           @Value("${internal.token}") String internalToken) {
        // X-Internal-Token proves to inventory-service that this is a trusted
        // internal call; without it, direct (non-gateway) requests are rejected.
        this.restClient = loadBalancedRestClientBuilder
                .baseUrl(inventoryServiceUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
        this.circuitBreaker = circuitBreakerFactory.create("inventory");
    }

    public boolean hasSufficientStock(Long productId, int quantity) {
        return circuitBreaker.run(() -> checkStock(productId, quantity), throwable -> {
            log.warn("Inventory availability check failed for product {}: {}", productId, throwable.getMessage());
            throw new InventoryUnavailableException(productId, throwable);
        });
    }

    private boolean checkStock(Long productId, int quantity) {
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
            throw e;
        }
    }
}
