package org.example.priceservice.service;

import org.example.priceservice.client.BinanceApiClient;
import org.example.priceservice.entity.PriceCandle;
import org.example.priceservice.repository.PriceCandleRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class PriceCandleService {
    private final PriceCandleRepository priceCandleRepository;

    public PriceCandleService(PriceCandleRepository priceCandleRepository) {
        this.priceCandleRepository = priceCandleRepository;
    }

    public List<PriceCandle> getCandles(String symbol, String interval, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "openTime"));

        // Gọi repository và trả về trực tiếp
        return priceCandleRepository.findBySymbolAndInterval(symbol.toUpperCase(), interval, pageable);
    }
}
