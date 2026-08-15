package com.example.overall.s4mvcarchitecture.repository;

import com.example.overall.s4mvcarchitecture.model.OrderReceipt;

public interface OrderRepository {
    OrderReceipt save(String itemName);
}
