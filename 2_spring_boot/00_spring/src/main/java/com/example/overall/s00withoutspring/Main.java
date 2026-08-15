package com.example.overall.s0withoutspring;

public class Main {
    public static void main(String[] args) {

        // Manual DI: the main method chooses the concrete implementation.
        LoggerService logger = new FileLogger();
        
        // Manual DI: the main method passes the dependency into OrderService.
        OrderService orderService = new OrderService(logger);
        orderService.placeOrder();

    }
}
