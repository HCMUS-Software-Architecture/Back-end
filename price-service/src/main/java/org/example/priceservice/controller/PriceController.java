package org.example.priceservice.controller;

import org.example.priceservice.entity.PriceCandle;
import org.example.priceservice.service.PriceCandleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prices")
public class PriceController {
    private final PriceCandleService candleService;

    public PriceController( PriceCandleService candleService) {
        this.candleService = candleService;
    }


    @GetMapping("/historical")
    public ResponseEntity<List<PriceCandle>> getHistoricalCandles(
            @RequestParam(defaultValue = "BTCUSDT") String symbol,
            @RequestParam(defaultValue = "1h") String interval,
            @RequestParam(defaultValue = "100") int limit
    ) {
        List<PriceCandle> candles = candleService.getCandles(symbol, interval, limit);
        return ResponseEntity.ok(candles);
    }

    @GetMapping("/symbols")
    public ResponseEntity<List<String>> getAvailableSymbols() {
        return ResponseEntity.ok(List.of("btcusdt", "ethusdt", "bnbusdt"));
    }

    @GetMapping("/intervals")
    public ResponseEntity<List<String>> getAvailableIntervals() {
        return ResponseEntity.ok(List.of("1m", "3m", "5m", "15m", "30m"));
    }
}
