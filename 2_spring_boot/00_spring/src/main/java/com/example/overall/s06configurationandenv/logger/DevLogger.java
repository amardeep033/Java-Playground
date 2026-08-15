package com.example.overall.s6configurationandenv.logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// @Profile avoids code such as if (environment.equals("dev")); Spring registers only this logger.
// @Value is convenient when a class needs a few individual configuration values.
@Component
@Profile("dev")
public class DevLogger implements LoggerService {
    private final String appName;
    private final String level;
    private final String destination;
    private final String format;

    public DevLogger(
            @Value("${logger.app-name}") String appName,
            @Value("${logger.level}") String level,
            @Value("${logger.destination}") String destination,
            @Value("${logger.format:PLAIN}") String format) {
        this.appName = appName;
        this.level = level;
        this.destination = destination;
        this.format = format;
    }

    @Override
    public void log(String message) {
        System.out.println("DEV logger using @Value");
        System.out.println("app-name = " + appName + " (default from application.properties)");
        System.out.println("level = " + level + " (profile file overrides base; CLI/env can override again)");
        System.out.println("destination = " + destination + " (defined directly in application-dev.properties)");
        System.out.println("format = " + format + " (code default from ${logger.format:PLAIN})");
        System.out.println("message = " + message);
    }
}
