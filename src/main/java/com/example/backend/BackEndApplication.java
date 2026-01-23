package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @deprecated MONOLITH APPLICATION - SAFE TO DELETE
 * 
 *             This monolith has been fully migrated to microservices:
 *             - api-gateway (port 8081) - Gateway, CORS, JWT filter
 *             - user-service (port 8082) - Auth, User management
 *             - price-service (port 8083) - Price data, WebSocket, Candles
 *             - discovery-server (port 8761) - Eureka service registry
 * 
 *             The entire Back-end/src/ folder can be deleted once migration is
 *             validated.
 *             Article service will be added as a separate microservice later.
 */
@Deprecated(forRemoval = true)
@SpringBootApplication
public class BackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackEndApplication.class, args);
    }

}
