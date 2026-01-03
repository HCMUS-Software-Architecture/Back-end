package com.example.backend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CandleDto {
    private String symbol;
    private long openTime;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
}
