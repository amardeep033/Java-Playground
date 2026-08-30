package com.example.basiccrud.config;

import com.example.basiccrud.model.Product;
import com.example.basiccrud.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Keeps the 02 sample products available after moving from in-memory storage to PostgreSQL.
// CommandLineRunner runs once after the Spring application context starts.
@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return; // Do not insert duplicates every time the app restarts.
        }

        productRepository.save(new Product(null, "Notebook", 80.0));
        productRepository.save(new Product(null, "Pen", 10.0));
    }
}
