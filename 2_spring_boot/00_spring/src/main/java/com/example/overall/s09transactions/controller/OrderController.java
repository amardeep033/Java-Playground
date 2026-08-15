package com.example.overall.s9transactions.controller;

import com.example.overall.s9transactions.service.OrderService;
import com.example.overall.s9transactions.service.OrderService2;
import org.springframework.stereotype.Controller;

@Controller
public class OrderController {
    private final OrderService orderService;
    private final OrderService2 orderService2;

    public OrderController(OrderService orderService, OrderService2 orderService2) {
        this.orderService = orderService;
        this.orderService2 = orderService2;
    }

    public void placeOrder() {
        System.out.println("C1. Controller -> Service proxy");
        try {
            orderService.placeOrder();
            // orderService2.placeOrder();
        } catch (Exception exception) {
            System.out.println("C2. caught " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }
}
