// package com.example.backend.config;\n\n/**\n * @deprecated This class is
// REDUNDANT after microservices migration.\n * CORS is now handled by the API
// Gateway
// (api-gateway/src/main/java/org/example/apigateway/config/SecurityConfig.java)\n
// * \n * This file can be safely deleted once you confirm the API Gateway CORS
// is working.\n * \n * DELETE THIS FILE:
// Back-end/src/main/java/com/example/backend/config/CorsConfig.java\n */
// \n @Deprecated(forRemoval=true)\n\nimport
// org.springframework.context.annotation.Bean;\nimport
// org.springframework.context.annotation.Configuration;\nimport
// org.springframework.web.cors.CorsConfiguration;\nimport
// org.springframework.web.cors.CorsConfigurationSource;\nimport
// org.springframework.web.cors.UrlBasedCorsConfigurationSource;\nimport
// org.springframework.web.servlet.config.annotation.CorsRegistry;\nimport
// org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// import java.util.List;

// @Configuration
// public class CorsConfig {
// @Bean
// public CorsConfigurationSource corsConfigurationSource() {
// CorsConfiguration config = new CorsConfiguration();
// config.setAllowedOrigins(List.of("http://localhost:3000",
// "http://localhost:63342"));
// config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
// config.setAllowedHeaders(List.of("*"));
// config.setAllowCredentials(true);
// config.setMaxAge(3600L);

// UrlBasedCorsConfigurationSource source = new
// UrlBasedCorsConfigurationSource();
// source.registerCorsConfiguration("/**", config);
// return source;
// }
// }
