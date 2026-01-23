package org.example.priceservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceKlineEvent {
    @JsonProperty("e") private String eventType; // "kline"
    @JsonProperty("E") private Long eventTime;
    @JsonProperty("s") private String symbol;
    @JsonProperty("k") private BinanceKlineData kline;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BinanceKlineData {
        @JsonProperty("t") private Long openTime;
        @JsonProperty("T") private Long closeTime;
        @JsonProperty("s") private String symbol;
        @JsonProperty("i") private String interval;
        @JsonProperty("f") private Long firstTradeId;
        @JsonProperty("L") private Long lastTradeId;
        @JsonProperty("o") private String open;
        @JsonProperty("c") private String close;
        @JsonProperty("h") private String high;
        @JsonProperty("l") private String low;
        @JsonProperty("v") private String volume;
        @JsonProperty("n") private Integer trades;
        @JsonProperty("x") private boolean isClosed; // Cờ quan trọng: true nếu nến đã đóng
        @JsonProperty("q") private String quoteVolume;
        @JsonProperty("V") private String takerBuyBaseVolume;
        @JsonProperty("Q") private String takerBuyQuoteVolume;
    }
}
