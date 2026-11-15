package com.example.overall.s2autoconfig;

import org.springframework.stereotype.Component;

@Component
public class FileLogger implements LoggerService {
    @Override
    public void log(String message) {
        System.out.println("File log: " + message);
    }
}
