package com.example.backend.service.collector;

import java.util.ArrayList;
import java.util.List;

public interface CollectorProvider {
    default List<String> getAllSymbols() {
        List<String> symbols = new ArrayList<>();
        symbols.add("btcusdt");
        symbols.add("ethusdt");
        symbols.add("bnbusdt");
        return symbols;
    }
    default List<String> getAllSupportedIntervals() {
        String[] supportedInterval = {"1m", "3m", "5m", "15m", "30m", "1h"};
        return List.of(supportedInterval);
    }
}
