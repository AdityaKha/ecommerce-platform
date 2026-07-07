package com.aditya.ecommerce.order.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer-side contract test: runs InventoryClient against WireMock stubs
 * generated at runtime from inventory-service's contract definitions
 * (inventory-service/src/test/resources/contracts), so a contract change that
 * breaks this client fails here rather than in production. The producer side
 * of the same contracts is verified by inventory-service's generated
 * ContractVerifierTest.
 */
@SpringBootTest(classes = InventoryClientContractTest.TestConfig.class, properties = {
        "stubrunner.generate-stubs=true",
        "eureka.client.enabled=false",
        "inventory.service.url=http://localhost:${stubrunner.runningstubs.inventory-service.port}"
})
@AutoConfigureStubRunner(
        stubsMode = StubRunnerProperties.StubsMode.REMOTE,
        ids = "com.aditya.ecommerce:inventory-service")
class InventoryClientContractTest {

    @DynamicPropertySource
    static void contractsLocation(DynamicPropertyRegistry registry) {
        // Surefire's working directory is the order-service module, so the
        // producer's contracts live one level up in this monorepo.
        Path contracts = Paths.get("..", "inventory-service", "src", "test", "resources", "contracts")
                .toAbsolutePath().normalize();
        registry.add("stubrunner.repository-root", () -> "stubs://" + contracts.toUri());
    }

    @Autowired
    private InventoryClient inventoryClient;

    @Test
    void hasSufficientStock_whenStubbedQuantityCoversRequest_returnsTrue() {
        assertThat(inventoryClient.hasSufficientStock(1L, 5)).isTrue();
    }

    @Test
    void hasSufficientStock_whenRequestExceedsStubbedQuantity_returnsFalse() {
        assertThat(inventoryClient.hasSufficientStock(1L, 100)).isFalse();
    }

    @Test
    void hasSufficientStock_whenProductUnknownPerContract_returnsFalse() {
        assertThat(inventoryClient.hasSufficientStock(999L, 1)).isFalse();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            KafkaAutoConfiguration.class
    })
    static class TestConfig {

        // Plain (non-load-balanced) builder: the stub runs on localhost, not
        // behind Eureka, and the contract under test is HTTP, not discovery.
        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        InventoryClient inventoryClient(RestClient.Builder builder,
                                        CircuitBreakerFactory<?, ?> circuitBreakerFactory,
                                        @Value("${inventory.service.url}") String url) {
            return new InventoryClient(builder, circuitBreakerFactory, url);
        }
    }
}
