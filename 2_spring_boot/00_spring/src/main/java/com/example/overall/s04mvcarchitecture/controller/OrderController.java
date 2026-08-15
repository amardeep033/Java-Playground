package com.example.overall.s4mvcarchitecture.controller;

import com.example.overall.s4mvcarchitecture.model.OrderReceipt;
import com.example.overall.s4mvcarchitecture.service.OrderService;
import org.springframework.stereotype.Controller;

// Controller-layer role: receives input from the outside world, delegates the use case to the service, and converts the result into a response.
@Controller
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public String placeOrder(String itemName) {
        System.out.println("B. Controller");

        OrderReceipt receipt = orderService.createOrder(itemName);
        System.out.println("E. Controller");

        return "Order " + receipt.orderId() + " created for " + receipt.itemName();
    }
}
