package com.example.overall.s0withoutspring;

public class FileLogger implements LoggerService {
    @Override
    public void log(String message) {
        System.out.println("File log: " + message);
    }
}
