package com.example.backend.repository;

import com.example.backend.entity.PriceTick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PriceTickRepository extends JpaRepository<PriceTick, UUID> {
    List<PriceTick> findBySymbolAndTimestampBetween(String symbol, Instant start, Instant end);

    @Modifying
    @Query("DELETE from PriceTick p where p.timestamp < :cutoff")
    void deleteByTimestampLessThan(Instant cutoff);
}
