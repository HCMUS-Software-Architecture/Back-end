package org.example.priceservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "price_candles")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PriceCandle {
    @Id
    private String id;

    private String symbol;

    private String interval; // 1m, 5m, 15m, 1h, 4h, 1d

    private Instant openTime;

    private Instant closeTime;

    private BigDecimal open;

    private BigDecimal high;

    private BigDecimal low;

    private BigDecimal close;

    private BigDecimal volume;

    private Integer trades;

    private LocalDateTime createdAt;
}
