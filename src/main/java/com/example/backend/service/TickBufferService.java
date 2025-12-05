package com.example.backend.service;

import com.example.backend.entity.PriceTick;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TickBufferService {
    private final Map<String, List<PriceTick>> tickBuffer = new ConcurrentHashMap<>();

    // Add tick to buffer memory
    public void addTick(PriceTick tick) {
        tickBuffer.computeIfAbsent(tick.getSymbol(), k -> new ArrayList<>()).add(tick);
    }

    // Get all ticks from buffer
    public Map<String, List<PriceTick>> drainBuffer() {
        Map<String, List<PriceTick>> snapshot = new ConcurrentHashMap<>();

        tickBuffer.keySet().forEach(symbol -> {
            List<PriceTick> ticks = tickBuffer.remove(symbol);
            if (ticks != null && !ticks.isEmpty()) {
                snapshot.put(symbol, ticks);
            }
        });

        return snapshot;
    }
}
