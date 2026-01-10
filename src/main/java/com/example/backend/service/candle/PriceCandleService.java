package com.example.backend.service.candle;

import com.example.backend.dto.CandleDto;
import com.example.backend.entity.PriceCandle;
import com.example.backend.entity.PriceTick;
import com.example.backend.repository.PriceCandleRepository;
import com.example.backend.service.TickBufferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceCandleService implements ICandleService {
    private final TickBufferService tickBufferService;
    private final PriceCandleRepository candleRepository;

    @Value("${price.symbols}")
    private String currencySymbols;

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final int[] supportedTime = {1, 5, 10, 15, 60, 240}; // 1m, 5m, 15m, 1h, 4h
    private final Map<String, PriceCandle> currentCandles = new ConcurrentHashMap<>();

    @Override
    public List<PriceCandle> getCandles(String symbol, String interval, int limit) {
        return candleRepository.findBySymbolAndIntervalOrderByOpenTimeDesc(
                symbol.toUpperCase(),
                interval,
                PageRequest.of(0, limit)
        );
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deletePriceCandlePeriodic(){
        LocalDateTime now = LocalDateTime.now().minusWeeks(1);
        candleRepository.deleteByCreatedAtBeforeOrCreatedAtIsNull(now);
    }
}
