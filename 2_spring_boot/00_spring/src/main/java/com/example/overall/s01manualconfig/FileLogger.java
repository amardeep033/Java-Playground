package com.example.overall.s1manualconfig;

// This implementation is exactly the same as the one in the example without Spring.
public class FileLogger implements LoggerService {
    @Override
    public void log(String message) {
        System.out.println("File log: " + message);
    }
}
