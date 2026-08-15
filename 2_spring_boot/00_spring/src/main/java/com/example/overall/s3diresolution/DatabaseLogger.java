package com.example.overall.s3diresolution;

import org.springframework.stereotype.Component;

@Component("xyz")
public class DatabaseLogger implements LoggerService {
    @Override
    public void log(String message) {
        System.out.println("Database: " + message);
    }
}
