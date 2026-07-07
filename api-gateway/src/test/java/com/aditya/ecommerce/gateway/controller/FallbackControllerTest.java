package com.aditya.ecommerce.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    void fallback_returns503WithJsonErrorBody() {
        ResponseEntity<Map<String, Object>> response = controller.fallback().block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody())
                .containsEntry("status", 503)
                .containsKey("error")
                .containsKey("message");
    }
}
