package com.example.overall.s2autoconfig;

import org.springframework.stereotype.Component;

// @Component tells Spring that this class should be discovered and managed as a bean.
// Because it implements LoggerService, Spring can inject it wherever LoggerService is required.
@Component
public class FileLogger implements LoggerService {
    @Override
    public void log(String message) {
        System.out.println("File log: " + message);
    }
}
