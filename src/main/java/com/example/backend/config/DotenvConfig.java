package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Configuration to load environment variables from micro.env file.
 * This allows separation of microservices environment config from other .env
 * files.
 * 
 * The me.paulschwarz:spring-dotenv library automatically loads .env files.
 * This configuration explicitly specifies micro.env as the source.
 */
@Configuration
@PropertySource(value = "file:micro.env", ignoreResourceNotFound = true)
public class DotenvConfig {
    // Spring will automatically load properties from micro.env
    // The library handles the parsing and injection into the environment
}
