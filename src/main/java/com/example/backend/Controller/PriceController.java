package com.example.backend.Controller;

import com.example.backend.entity.PriceCandle;
import com.example.backend.service.PriceCandleService;
import com.example.backend.service.PriceCollectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {
    private final PriceCollectorService priceCollectorService;
    private final PriceCandleService candleService;

    @GetMapping("/current/{symbol}")
    public ResponseEntity<Map<String, Object>> getCurrentPrice(@PathVariable String symbol) {
        BigDecimal price = priceCollectorService.getLatestPrice(symbol.toUpperCase());
        if (price == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "symbol", symbol.toUpperCase(),
                "price", price,
                "timestamp", System.currentTimeMillis()
        ));
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
        return ResponseEntity.ok(List.of("BTCUSDT", "ETHUSDT", "BNBUSDT"));
    }
}

