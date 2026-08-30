package com.example.basiccrud.repository;

import com.example.basiccrud.model.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// In 02 this was a hand-written in-memory repository backed by ConcurrentHashMap.
// In 03 this interface extends JpaRepository, and Spring Data JPA creates the implementation automatically.
// JpaRepository<Product, Long> means: manage Product entities whose primary key type is Long.

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Spring Data parses this method name and generates a SQL query similar to:
    // where lower(name) like lower('%name%') and price >= minPrice
    List<Product> findByNameContainingIgnoreCaseAndPriceGreaterThanEqual(String name, double minPrice);
}
