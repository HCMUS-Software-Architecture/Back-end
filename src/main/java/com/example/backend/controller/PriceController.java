// package com.example.backend.controller;

// import com.example.backend.entity.PriceCandle;
// import com.example.backend.service.candle.ICandleService;
// import com.example.backend.service.collector.CollectorProvider;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;

// /**
// * @deprecated SAFE TO DELETE - Migrated to price-service
// * @see
// price-service/src/main/java/org/example/priceservice/controller/PriceController.java
// */
// @Deprecated(forRemoval = true)
// @RestController
// @RequestMapping("/api/prices")
// public class PriceController {
// private final CollectorProvider priceCollectorService;
// private final ICandleService candleService;

// public PriceController(CollectorProvider priceCollectorService,
// ICandleService candleService) {
// this.priceCollectorService = priceCollectorService;
// this.candleService = candleService;
// }

// @GetMapping("/historical")
// public ResponseEntity<List<PriceCandle>> getHistoricalCandles(
// @RequestParam(defaultValue = "BTCUSDT") String symbol,
// @RequestParam(defaultValue = "1h") String interval,
// @RequestParam(defaultValue = "100") int limit) {
// List<PriceCandle> candles = candleService.getCandles(symbol, interval,
// limit);
// return ResponseEntity.ok(candles);
// }

// @GetMapping("/symbols")
// public ResponseEntity<List<String>> getAvailableSymbols() {
// return ResponseEntity.ok(priceCollectorService.getAllSymbols());
// }

// @GetMapping("/intervals")
// public ResponseEntity<List<String>> getAvailableIntervals() {
// return ResponseEntity.ok(priceCollectorService.getAllSupportedIntervals());
// }
// }
