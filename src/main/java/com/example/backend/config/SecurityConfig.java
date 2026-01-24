// package com.example.backend.config;

// import com.example.backend.filter.JwtAuthFilter;
// import com.example.backend.service.JwtService;
// import lombok.RequiredArgsConstructor;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.Customizer;
// import
// org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import
// org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import
// org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;
// import
// org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// /**
// * @deprecated SAFE TO DELETE - Security migrated to each microservice
// * - api-gateway: handles CORS and JWT validation
// * - user-service: has own SecurityConfig for password encoding
// * - price-service: has own SecurityConfig
// * @see
// api-gateway/src/main/java/org/example/apigateway/config/SecurityConfig.java
// */
// @Deprecated(forRemoval = true)
// @Configuration
// @EnableWebSecurity
// @RequiredArgsConstructor
// public class SecurityConfig {
// private final JwtAuthFilter jwtAuthFilter;

// @Bean
// public SecurityFilterChain securityFilterChain(HttpSecurity http) throws
// Exception {
// return http.csrf(AbstractHttpConfigurer::disable)
// .cors(Customizer.withDefaults())
// .authorizeHttpRequests(auth -> auth
// .requestMatchers("/api/auth/register", "/api/auth/login",
// "/api/auth/refresh").permitAll()
// .requestMatchers("/api/health/**").permitAll()
// .requestMatchers("/v3/**", "/swagger-ui/**").permitAll()
// .requestMatchers("/ws/**").permitAll() // further security consideration
// needed
// .anyRequest().authenticated())
// .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
// .build();

// }

// @Bean
// public PasswordEncoder passwordEncoder() {
// return new BCryptPasswordEncoder();
// }
// }
