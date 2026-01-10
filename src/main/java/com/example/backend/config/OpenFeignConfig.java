package com.example.backend.config;

import com.example.backend.service.candle.ExternalPriceCandleService;
import com.example.backend.service.candle.PriceCandleService;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackageClasses = ExternalPriceCandleService.class)
public class OpenFeignConfig {
}
