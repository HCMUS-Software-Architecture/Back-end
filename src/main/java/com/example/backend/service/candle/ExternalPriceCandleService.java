package com.example.backend.service.candle;

import com.example.backend.entity.PriceCandle;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Primary
public class ExternalPriceCandleService implements ICandleService {
    private final BinanceApiClient binanceApiClient;

    public ExternalPriceCandleService(BinanceApiClient binanceApiClient) {
        this.binanceApiClient = binanceApiClient;
    }

    private PriceCandle candleMapper(List<Object> rawData, String symbol, String interval) {
        PriceCandle candle = new PriceCandle();

        candle.setSymbol(symbol);
        candle.setInterval(interval);
        candle.setCreatedAt(LocalDateTime.now());

        candle.setOpenTime(Instant.ofEpochMilli(convertToLong(rawData.get(0))));

        candle.setOpen(new BigDecimal(rawData.get(1).toString()));

        candle.setHigh(new BigDecimal(rawData.get(2).toString()));

        candle.setLow(new BigDecimal(rawData.get(3).toString()));

        candle.setClose(new BigDecimal(rawData.get(4).toString()));

        candle.setVolume(new BigDecimal(rawData.get(5).toString()));

        candle.setCloseTime(Instant.ofEpochMilli(convertToLong(rawData.get(6))));

        candle.setTrades(convertToInteger(rawData.get(8)));

        return candle;
    }

    private Long convertToLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return Long.parseLong(obj.toString());
    }

    private Integer convertToInteger(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        return Integer.parseInt(obj.toString());
    }

    @Override
    public List<PriceCandle> getCandles(String symbol, String interval, int limit) {
        List<List<Object>> binanceResponse = binanceApiClient.getAllCandles(symbol.toUpperCase(), interval, limit);
        List<PriceCandle> priceCandles = new ArrayList<>();

        for (List<Object> rawData : binanceResponse) {
            PriceCandle candle = candleMapper(rawData, symbol, interval);
            priceCandles.add(candle);
        }
        return priceCandles;
    }
}
