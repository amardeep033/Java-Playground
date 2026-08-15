package com.example.overall.s3diresolution;

import org.springframework.stereotype.Component;

@Component
// @Primary
public class ConsoleLogger implements LoggerService {
    @Override
    public void log(String message) {
        System.out.println("Console: " + message);
    }
}
