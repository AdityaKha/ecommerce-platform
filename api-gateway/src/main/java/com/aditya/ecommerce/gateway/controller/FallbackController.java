package com.aditya.ecommerce.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Target of every route's CircuitBreaker fallbackUri. Reached when a downstream
 * call fails, times out, or the circuit is open — returns the same JSON error
 * shape as GlobalErrorHandlingFilter, but with 503 to signal "retry later".
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public Mono<ResponseEntity<Map<String, Object>>> fallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "message", "Downstream service is unavailable or not responding; please retry later"
        )));
    }
}
