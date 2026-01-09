package com.example.backend.service.candle;

import com.example.backend.entity.PriceCandle;

import java.util.List;

public interface ICandleService {
    List<PriceCandle> getCandles(String symbol, String interval, int limit);
}
