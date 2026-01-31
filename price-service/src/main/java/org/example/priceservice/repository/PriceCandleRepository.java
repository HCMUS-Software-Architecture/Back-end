package org.example.priceservice.repository;

import org.example.priceservice.entity.PriceCandle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface PriceCandleRepository extends MongoRepository<PriceCandle, String> {
    // Query với case-insensitive cho symbol (dùng regex)
    @Query("{ 'symbol': { $regex: ?0, $options: 'i' }, 'interval': ?1 }")
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