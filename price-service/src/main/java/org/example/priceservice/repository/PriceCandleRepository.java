package org.example.priceservice.repository;

import org.example.priceservice.entity.PriceCandle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface PriceCandleRepository extends MongoRepository<PriceCandle, String> {
    List<PriceCandle> findBySymbolAndInterval(String symbol, String interval, Pageable pageable);

    List<PriceCandle> findBySymbolAndIntervalAndOpenTimeBetween(
            String symbol,
            String interval,
            Instant startTime,
            Instant endTime);

    void deleteByCreatedAtBeforeOrCreatedAtIsNull(LocalDateTime createdAtBefore);

    /**
     * Find the candle closest to but not after the given timestamp
     * Used for getting price at a specific point in time
     */
}