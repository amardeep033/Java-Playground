package com.example.overall.s8validation.service;

import com.example.overall.s8validation.dto.OrderRequest;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    public void placeOrder(OrderRequest request) {
        System.out.println("S1. Service creates order for " + request.notnullitem());
    }
}
