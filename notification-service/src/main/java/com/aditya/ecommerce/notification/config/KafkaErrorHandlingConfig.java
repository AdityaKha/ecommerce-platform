package com.aditya.ecommerce.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Retries transient consumer failures (SMTP provider down, broker hiccups)
 * with exponential backoff before giving up and logging the failed record.
 * Boot's Kafka auto-configuration wires this handler into the default
 * listener container factory.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(4);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);
        return new DefaultErrorHandler(backOff);
    }
}
