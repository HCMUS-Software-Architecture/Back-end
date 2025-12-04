package com.example.backend.repository;

import com.example.backend.entity.PriceCandle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PriceCandleRepository extends JpaRepository<PriceCandle, UUID> {
    List<PriceCandle> findBySymbolAndIntervalOrderByOpenTimeDesc(String symbol, String interval, Pageable pageable);
}
