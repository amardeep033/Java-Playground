package com.example.overall.s4mvcarchitecture.repository;

import com.example.overall.s4mvcarchitecture.model.OrderReceipt;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Repository;


// Repository-layer role: hides persistence details from the service; this implementation stores nothing permanently and only simulates generated order IDs in memory.
@Repository
public class InMemoryOrderRepository implements OrderRepository {
    private final AtomicInteger sequence = new AtomicInteger(100);

    @Override
    public OrderReceipt save(String itemName) {
        System.out.println("D: Repository");
        return new OrderReceipt(sequence.incrementAndGet(), itemName);
    }
}
