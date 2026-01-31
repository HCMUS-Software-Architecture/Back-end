package org.example.priceservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.priceservice.client.BinanceApiClient;
import org.example.priceservice.entity.PriceCandle;
import org.example.priceservice.repository.PriceCandleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("collector")
public class CandlesSaving {
    private final PriceCandleRepository priceCandleRepository;
    private final BinanceApiClient binanceApiClient;
    private final CandleMapper candleMapper;
    private final MongoTemplate mongoTemplate;
    private final Executor candleTaskExecutor;

    @Value("${price.symbols:btcusdt,ethusdt}")
    private String symbolsConfig;
    private final String[] supportedInterval = { "1m", "3m", "5m", "15m", "30m", "1h" };

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        CompletableFuture.runAsync(this::saveCandles, candleTaskExecutor);
    }

    public void saveCandles() {

        String[] symbols = symbolsConfig.split(","); // Ví dụ: BTCUSDT, ETHUSDT...
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String symbol : symbols) {
            for (String interval : supportedInterval) {
                // Tạo một async task cho mỗi cặp (Symbol + Interval)
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    processSymbolInterval(symbol, interval);
                }, candleTaskExecutor);
                futures.add(future);
            }
        }

        // Chờ tất cả các luồng chạy xong
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        System.out.println("Finished syncing candles.");
    }

    // Hàm xử lý logic cho 1 cặp duy nhất
    private void processSymbolInterval(String symbol, String interval) {
        try {
            log.info("Fetching candles for {} {} - limit: 1000", symbol, interval);
            List<List<Object>> rawObjects = binanceApiClient.getAllCandles(symbol.toUpperCase(), interval, 1000);
            log.info("Received {} candles for {} {}", rawObjects.size(), symbol, interval);

            if (rawObjects == null || rawObjects.isEmpty()) {
                log.warn("No candles received for {} {}", symbol, interval);
                return;
            }

            // 2. Map dữ liệu
            List<PriceCandle> candles = new ArrayList<>();
            for (List<Object> raw : rawObjects) {
                candles.add(candleMapper.map(raw, symbol, interval));
            }

            // 3. Bulk Upsert vào MongoDB (Hiệu năng cao + Chống trùng lặp)
            bulkUpsert(candles);
            log.info("Successfully saved {} candles for {} {}", candles.size(), symbol, interval);

        } catch (Exception e) {
            // Log lỗi để không ảnh hưởng các luồng khác
            log.error("Error fetching {} {}: {}", symbol, interval, e.getMessage(), e);
        }
    }

    private void bulkUpsert(List<PriceCandle> candles) {
        if (candles.isEmpty())
            return;

        // BulkOperations giúp gom nhiều lệnh lại gửi 1 lần xuống Mongo
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, PriceCandle.class);

        for (PriceCandle candle : candles) {
            // Định nghĩa điều kiện tìm kiếm (Symbol + Interval + OpenTime là duy nhất)
            Query query = new Query();
            query.addCriteria(Criteria.where("symbol").is(candle.getSymbol().toUpperCase())
                    .and("interval").is(candle.getInterval())
                    .and("openTime").is(candle.getOpenTime()));

            // Dữ liệu cần update
            Update update = new Update()
                    .set("open", candle.getOpen())
                    .set("high", candle.getHigh())
                    .set("low", candle.getLow())
                    .set("close", candle.getClose())
                    .set("volume", candle.getVolume())
                    .set("closeTime", candle.getCloseTime())
                    .set("trades", candle.getTrades())
                    .set("updatedAt", LocalDateTime.now()) // Nên có field này để biết record được cập nhật khi nào
                    .setOnInsert("createdAt", LocalDateTime.now()); // Chỉ set createdAt khi insert mới

            // Thêm vào hàng đợi Upsert
            bulkOps.upsert(query, update);
        }

        // Thực thi
        bulkOps.execute();
    }
}
