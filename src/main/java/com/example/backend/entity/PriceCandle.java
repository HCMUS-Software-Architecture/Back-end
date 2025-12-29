package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "price_candles")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PriceCandle {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 10)
    private String interval; // 1m, 5m, 15m, 1h, 4h, 1d

    @Column(nullable = false)
    private Instant openTime;

    private Instant closeTime;

    @Column(precision = 20, scale = 8)
    private BigDecimal open;

    @Column(precision = 20, scale = 8)
    private BigDecimal high;

    @Column(precision = 20, scale = 8)
    private BigDecimal low;

    @Column(precision = 20, scale = 8)
    private BigDecimal close;

    @Column(precision = 30, scale = 8)
    private BigDecimal volume;

    private Integer trades;

    private LocalDateTime createdAt;
}
