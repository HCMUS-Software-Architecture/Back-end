package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "price_ticks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceTick {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false)
    private Instant timestamp;

    private String exchange;
}
