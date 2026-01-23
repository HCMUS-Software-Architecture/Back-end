package org.example.priceservice.config;

import org.example.priceservice.client.BinanceApiClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackageClasses = BinanceApiClient.class)
public class OpenFeignConfig {
}
