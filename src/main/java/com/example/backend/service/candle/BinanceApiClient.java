package com.example.backend.service.candle;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "binanceApiClient", url = "https://api1.binance.com")
public interface BinanceApiClient {
    @GetMapping("/api/v3/klines")
    List<List<Object>> getAllCandles(@RequestParam(defaultValue = "BTCUSDT") String symbol,
                                     @RequestParam(defaultValue = "1m") String interval,
                                     @RequestParam(defaultValue = "20") int limit);
}
