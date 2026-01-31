package org.example.priceservice.service;

import lombok.extern.slf4j.Slf4j;
import org.example.priceservice.client.BinanceApiClient;
import org.example.priceservice.entity.PriceCandle;
import org.example.priceservice.repository.PriceCandleRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class PriceCandleService {
    private final PriceCandleRepository priceCandleRepository;

    public PriceCandleService(PriceCandleRepository priceCandleRepository) {
        this.priceCandleRepository = priceCandleRepository;
    }

    public List<PriceCandle> getCandles(String symbol, String interval, int limit) {
        String normalizedSymbol = symbol.toUpperCase();
        
        log.info("Fetching candles for symbol={}, interval={}, limit={}", normalizedSymbol, interval, limit);
        
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "openTime"));

        // Query lấy data mới nhất trước (DESC)
        List<PriceCandle> candles = priceCandleRepository.findBySymbolAndInterval(normalizedSymbol, interval, pageable);
        
        log.info("Found {} candles in database", candles.size());
        
        if (!candles.isEmpty()) {
            log.debug("First candle time: {}, Last candle time: {}", 
                candles.get(0).getOpenTime(), 
                candles.get(candles.size() - 1).getOpenTime());
        }
        
        // Reverse để trả về theo thứ tự thời gian tăng dần (cũ -> mới) cho FE
        Collections.reverse(candles);
        
        return candles;
    }
}
