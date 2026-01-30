package org.example.priceservice.service;

import org.example.priceservice.entity.PriceCandle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Profile("collector")
public class CandleMapper {
    public PriceCandle map(List<Object> rawData, String symbol, String interval) {
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
}
