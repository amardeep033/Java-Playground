package com.example.basiccrud.service;

import com.example.basiccrud.model.Product;
import com.example.basiccrud.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;


@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 1.1 Service step for GET all products: ask the repository for every product.
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // 1.2 Service step for GET by id: Optional represents found or not found.
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    // 1.3 Service step for query-param search: ask the repository for products matching optional filters.
    public List<Product> searchProducts(String name, double minPrice) {
        return productRepository.searchProducts(name, minPrice);
    }

    // 2 Service step for POST: clear the id so the repository treats it as a new product.
    public Product createProduct(Product product) {
        product.setId(null); // Force a new id even if the client sends an id in the request body.
        return productRepository.save(product);
    }

    // 3 Service step for PUT: update only when the product already exists.
    public Optional<Product> updateProduct(Long id, Product product) {
        if (!productRepository.existsById(id)) {
            return Optional.empty();
        }

        product.setId(id);
        return Optional.of(productRepository.save(product));
    }

    // 4 Service step for DELETE: return true when deleted, false when the id was missing.
    public boolean deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            return false;
        }

        productRepository.deleteById(id);
        return true;
    }
}

// In this project, ProductRepository is a hand-written in-memory repository, so generic method names are clearer.
// In Spring Data JPA, long names like findByNameContainingIgnoreCaseAndPriceGreaterThanEqual(...) can auto-generate queries.
