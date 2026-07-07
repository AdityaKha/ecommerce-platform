package com.aditya.ecommerce.order.config;

import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // Built through RestClientBuilderConfigurer (not a bare RestClient.builder())
    // so Boot's customizers apply — in particular observation instrumentation,
    // which records a client span and propagates the traceparent header on
    // calls to inventory-service.
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }
}
