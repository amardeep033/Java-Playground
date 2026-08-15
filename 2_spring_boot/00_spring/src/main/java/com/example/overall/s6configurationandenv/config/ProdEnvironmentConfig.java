package com.example.overall.s6configurationandenv.config;

import com.example.overall.s6configurationandenv.properties.ProdLoggerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// The class name ProdEnvironmentConfig is ordinary; @Profile("prod") is what makes it conditional.
// application.properties imports app-prod.yaml, and that YAML activates its values only for prod.
@Configuration
@Profile("prod")
// Registers ProdLoggerProperties as a bean and binds the final resolved logger.* values into it.
@EnableConfigurationProperties(ProdLoggerProperties.class)
public class ProdEnvironmentConfig {
}
