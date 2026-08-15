package com.example.overall.s4mvcarchitecture.service;

import com.example.overall.s4mvcarchitecture.model.OrderReceipt;
import com.example.overall.s4mvcarchitecture.repository.OrderRepository;
import org.springframework.stereotype.Service;

// Service-layer role: coordinates the application use case and contains business flow that should not belong to HTTP handling or persistence code.
@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderReceipt createOrder(String itemName) {
        System.out.println("C: Service");
        return orderRepository.save(itemName);
    }
}
