package org.example.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    // Read from environment with fallback to localhost:3000
    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOriginsString;

    @Value("${cors.max-age:3600}")
    private Long maxAge;

    // Fallback origins if env parsing fails
    private static final List<String> FALLBACK_ORIGINS = List.of(
            "http://localhost:3000",
            "http://localhost:63342");

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) throws Exception {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(auth -> auth
                        .anyExchange().permitAll())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse comma-separated origins from env, use fallback if empty/null
        List<String> origins = parseOrigins(allowedOriginsString);
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> parseOrigins(String originsString) {
        if (originsString == null || originsString.isBlank()) {
            return FALLBACK_ORIGINS;
        }
        List<String> parsed = Arrays.stream(originsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return parsed.isEmpty() ? FALLBACK_ORIGINS : parsed;
    }
}
