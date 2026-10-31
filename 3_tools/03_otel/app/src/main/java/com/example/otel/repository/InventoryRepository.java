package com.example.otel.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class InventoryRepository {
    // A tiny read-only stand-in for a database. No database installation needed.
    private final Map<String, Integer> stock = Map.of("coffee", 10);

    public int available(String item) {
        return stock.getOrDefault(item, 0);
    }
}
