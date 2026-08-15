package com.example.overall.s6configurationandenv.logger;

// OrderService depends on this common abstraction; the active profile decides which implementation exists.
public interface LoggerService {
    void log(String message);
}
