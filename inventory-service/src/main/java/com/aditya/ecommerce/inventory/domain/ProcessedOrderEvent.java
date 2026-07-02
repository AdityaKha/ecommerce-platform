package com.aditya.ecommerce.inventory.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Records which order-created events have already been consumed, so a duplicate
 * Kafka delivery (at-least-once semantics) does not reserve stock twice.
 */
@Entity
@Table(name = "processed_order_events", uniqueConstraints = {
        @UniqueConstraint(columnNames = "orderId")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedOrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Instant processedAt;
}
