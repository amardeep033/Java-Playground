package com.example.overall.s6configurationandenv.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// @ConfigurationProperties is a Spring Boot feature that binds the complete logger.* group.
// JavaBean getters/setters are required here so the binder can populate the mutable properties object.
@ConfigurationProperties(prefix = "logger")
public class ProdLoggerProperties {
    private String appName;
    private String level;
    private String destination;
    private String format = "PLAIN";

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
