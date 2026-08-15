package com.example.overall.s6configurationandenv.logger;

import com.example.overall.s6configurationandenv.properties.ProdLoggerProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// prod demonstrates grouped, type-safe binding with @ConfigurationProperties.
// @Profile selects this implementation without an environment-name if/else inside OrderService.
@Component
@Profile("prod")
public class ProdLogger implements LoggerService {
    private final ProdLoggerProperties properties;

    public ProdLogger(ProdLoggerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void log(String message) {
        System.out.println("PROD logger using @ConfigurationProperties");
        System.out.println("app-name = " + properties.getAppName() + " (default from application.properties)");
        System.out.println("level = " + properties.getLevel() + " (app-prod.yaml overrides base; CLI/env can override again)");
        System.out.println("destination = " + properties.getDestination() + " (defined directly in app-prod.yaml)");
        System.out.println("format = " + properties.getFormat() + " (default initialized in Java code)");
        System.out.println("message = " + message);
    }
}
