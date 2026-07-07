package com.aditya.ecommerce.inventory.config;

import com.aditya.ecommerce.inventory.exception.InsufficientStockException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.util.NoSuchElementException;

/**
 * Retries transient consumer failures (DB unavailable, broker hiccups) with
 * exponential backoff before giving up and logging the failed record. Boot's
 * Kafka auto-configuration wires this handler into the default listener
 * container factory.
 *
 * Business rejections that can never succeed on redelivery — insufficient
 * stock, no inventory record for the product — are not retried and are
 * dropped after a single failure, preserving the Day-6 "log and drop rather
 * than redeliver forever" behavior.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(4);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(backOff);
        handler.addNotRetryableExceptions(InsufficientStockException.class, NoSuchElementException.class);
        return handler;
    }
}
