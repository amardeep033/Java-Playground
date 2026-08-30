package com.example.basiccrud.service;

import com.example.basiccrud.exception.ProductNotFoundException;
import com.example.basiccrud.model.Product;
import com.example.basiccrud.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;


@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Missing data becomes a custom exception; GlobalExceptionHandler converts it to HTTP 404.

    // This behavior stays from 02: service throws when the product is missing.
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        // findById now executes a database SELECT through Spring Data JPA instead of reading from a map.
    }

    public List<Product> searchProducts(String name, double minPrice) {
        String searchText = name == null ? "" : name;
        return productRepository.findByNameContainingIgnoreCaseAndPriceGreaterThanEqual(searchText, minPrice);
        // Spring Data JPA derives the query from the repository method name.
    }

    public Product createProduct(Product product) {
        product.setId(null);
        return productRepository.save(product);
        // null id + GenerationType.IDENTITY means PostgreSQL generates the primary key during INSERT.
    }

    public Product updateProduct(Long id, Product product) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }

        product.setId(id);
        return productRepository.save(product);
        // Existing id means JPA updates the existing row instead of inserting a brand-new row.
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }

        productRepository.deleteById(id);
    }
}
