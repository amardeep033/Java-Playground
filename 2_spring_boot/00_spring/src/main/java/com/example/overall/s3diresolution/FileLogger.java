package com.example.overall.s3diresolution;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class FileLogger implements LoggerService {
    @Override
    public void log(String message) {
        System.out.println("File: " + message);
    }
}
