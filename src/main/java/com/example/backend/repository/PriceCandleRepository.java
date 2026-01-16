package com.example.backend.repository;

import com.example.backend.entity.PriceCandle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriceCandleRepository extends JpaRepository<PriceCandle, UUID> {
    List<PriceCandle> findBySymbolAndIntervalOrderByOpenTimeDesc(String symbol, String interval, Pageable pageable);
    List<PriceCandle> findBySymbolAndIntervalAndOpenTimeBetween(
            String symbol,
            String interval,
            Instant startTime,
            Instant endTime
    );

    void deleteByCreatedAtBeforeOrCreatedAtIsNull(LocalDateTime createdAtBefore);
    
    /**
     * Find the candle closest to but not after the given timestamp
     * Used for getting price at a specific point in time
     */
    @Query("SELECT c FROM PriceCandle c WHERE c.symbol = :symbol AND c.interval = :interval " +
           "AND c.openTime <= :openTime ORDER BY c.openTime DESC LIMIT 1")
    Optional<PriceCandle> findTopBySymbolAndIntervalAndOpenTimeLessThanEqualOrderByOpenTimeDesc(
            @Param("symbol") String symbol,
            @Param("interval") String interval,
            @Param("openTime") Instant openTime);
}
