package com.example.overall.s6configurationandenv.config;

import com.example.overall.s6configurationandenv.logger.LoggerService;
import com.example.overall.s6configurationandenv.service.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// @Configuration marks this class as a source of bean definitions.
@Configuration
@ComponentScan("com.example.overall.s6configurationandenv")
public class AppConfig {

    // @Bean is useful for explicit construction and for classes that cannot carry @Component,
    // especially third-party clients. OrderService is created this way only to demonstrate it.
    @Bean
    public OrderService orderService(LoggerService loggerService) {
        return new OrderService(loggerService);
    }
}
