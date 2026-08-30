package com.example.basiccrud.config;

import com.example.basiccrud.model.Product;
import com.example.basiccrud.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// CommandLineRunner is generic Spring Boot startup code, not database-specific.
// Here we use it for database seeding because it runs after Spring creates ProductRepository.
@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        // count() is inherited from JpaRepository.
        // Spring Data JPA calls Hibernate, and Hibernate executes SQL similar to: select count(*) from products.
        if (productRepository.count() > 0) {
            return; // Do not insert duplicates every time the app restarts.
        }

        // Table creation happens before this runner executes.
        // During startup, Hibernate reads @Entity/@Table/@Column metadata and ddl-auto=update creates or updates the products table.
        // After that, this runner can safely insert rows through productRepository.save(...).
        productRepository.save(new Product(null, "Notebook", 80.0));
        productRepository.save(new Product(null, "Pen", 10.0));
    }
}
