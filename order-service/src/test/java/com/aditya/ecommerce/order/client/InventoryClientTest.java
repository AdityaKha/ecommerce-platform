package com.aditya.ecommerce.order.client;

import com.aditya.ecommerce.order.exception.InventoryUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InventoryClientTest {

    private static final String INVENTORY_URL = "http://inventory-service";

    private MockRestServiceServer mockServer;
    private InventoryClient inventoryClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        inventoryClient = new InventoryClient(builder, circuitBreakerFactory(), INVENTORY_URL);
    }

    /**
     * Small sliding window so the open-circuit path is reachable with few calls;
     * generous time limit so slow CI machines don't trip spurious timeouts.
     */
    private Resilience4JCircuitBreakerFactory circuitBreakerFactory() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build());
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .build());

        Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory(
                circuitBreakerRegistry, timeLimiterRegistry, null, new Resilience4JConfigurationProperties());
        factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(circuitBreakerRegistry.getDefaultConfig())
                .timeLimiterConfig(timeLimiterRegistry.getDefaultConfig())
                .build());
        return factory;
    }

    @Test
    void hasSufficientStock_whenQuantityAvailableCoversRequest_returnsTrue() {
        mockServer.expect(requestTo(INVENTORY_URL + "/api/inventory/1"))
                .andRespond(withSuccess("{\"id\": 1, \"productId\": 1, \"quantityAvailable\": 10}", MediaType.APPLICATION_JSON));

        boolean result = inventoryClient.hasSufficientStock(1L, 5);

        assertThat(result).isTrue();
    }

    @Test
    void hasSufficientStock_whenRequestExceedsAvailable_returnsFalse() {
        mockServer.expect(requestTo(INVENTORY_URL + "/api/inventory/1"))
                .andRespond(withSuccess("{\"id\": 1, \"productId\": 1, \"quantityAvailable\": 10}", MediaType.APPLICATION_JSON));

        boolean result = inventoryClient.hasSufficientStock(1L, 20);

        assertThat(result).isFalse();
    }

    @Test
    void hasSufficientStock_whenProductNotFound_returnsFalse() {
        mockServer.expect(requestTo(INVENTORY_URL + "/api/inventory/1"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        boolean result = inventoryClient.hasSufficientStock(1L, 5);

        assertThat(result).isFalse();
    }

    @Test
    void hasSufficientStock_whenServerError_throwsInventoryUnavailable() {
        mockServer.expect(requestTo(INVENTORY_URL + "/api/inventory/1"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> inventoryClient.hasSufficientStock(1L, 5))
                .isInstanceOf(InventoryUnavailableException.class);
    }

    @Test
    void hasSufficientStock_whenCircuitOpens_failsFastWithoutCallingService() {
        mockServer.expect(times(2), requestTo(INVENTORY_URL + "/api/inventory/1"))
                .andRespond(withServerError());

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> inventoryClient.hasSufficientStock(1L, 5))
                    .isInstanceOf(InventoryUnavailableException.class);
        }

        // Circuit is now open: this call must not reach the (exhausted) mock server.
        assertThatThrownBy(() -> inventoryClient.hasSufficientStock(1L, 5))
                .isInstanceOf(InventoryUnavailableException.class);

        mockServer.verify();
    }
}
