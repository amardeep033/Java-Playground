package com.example.basiccrud.repository;

import com.example.basiccrud.model.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

// Repository is almost unchanged from 01; DTOs stay outside this layer, and a real app would usually persist an entity/model here.

@Repository
public class ProductRepository {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong();

    public ProductRepository() {
        save(new Product(null, "Notebook", 80.0));
        save(new Product(null, "Pen", 10.0));
    }

    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }

    // Null-safe search keeps repository stable even if invalid data already exists.
    public List<Product> searchProducts(String name, double minPrice) {
        String searchText = name == null ? "" : name.toLowerCase();
        return products.values().stream()
                .filter(product -> product.getName() != null)
                .filter(product -> product.getName().toLowerCase().contains(searchText))
                .filter(product -> product.getPrice() >= minPrice)
                .toList();
    }

    public Product save(Product product) {
        Long id = product.getId();

        if (id == null) {
            id = idSequence.incrementAndGet();
            product.setId(id);
        }

        products.put(id, product);
        return product;
    }

    public boolean existsById(Long id) {
        return products.containsKey(id);
    }

    public void deleteById(Long id) {
        products.remove(id);
    }
}
