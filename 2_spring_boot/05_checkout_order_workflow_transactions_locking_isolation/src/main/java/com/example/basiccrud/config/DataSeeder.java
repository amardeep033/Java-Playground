package com.example.basiccrud.config;

import com.example.basiccrud.model.ProductInventory;
import com.example.basiccrud.repository.ProductInventoryRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductInventoryRepository productRepository;

    public DataSeeder(ProductInventoryRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }

        productRepository.save(new ProductInventory(null, "SKU-KEYBOARD", "Mechanical Keyboard", new BigDecimal("4500.00"), 5));
        productRepository.save(new ProductInventory(null, "SKU-MOUSE", "Wireless Mouse", new BigDecimal("900.00"), 3));
        productRepository.save(new ProductInventory(null, "SKU-BOOK", "Spring Boot Interview Guide", new BigDecimal("800.00"), 10));
    }
}
