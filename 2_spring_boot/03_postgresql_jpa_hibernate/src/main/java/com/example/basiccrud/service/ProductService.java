package com.example.basiccrud.service;

import com.example.basiccrud.dto.ProductSummary;
import com.example.basiccrud.exception.ProductNotFoundException;
import com.example.basiccrud.model.Product;
import com.example.basiccrud.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Same service role as 02: business flow stays here, while storage details stay in ProductRepository.
// A few method bodies changed only because repository operations now execute database queries through JPA.

// Why @Transactional is now visible in this service:
// In 02, data was in memory, so there was no real database transaction.
// In 03, repository methods talk to PostgreSQL through Hibernate, so service methods are a good place to define the unit of database work.

// readOnly = true is used on read methods to say "this method should not change data".
// It also lets Spring/Hibernate apply read-focused behavior. Do not rely on it as a security feature.

// Plain @Transactional is used on write methods so insert/update/delete work inside one transaction.
// It is also what keeps loaded entities managed long enough for Hibernate dirty checking.
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        // findById now executes a database SELECT through Spring Data JPA instead of reading from a map.
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String name, double minPrice) {
        String searchText = name == null ? "" : name;
        return productRepository.findByNameContainingIgnoreCaseAndPriceGreaterThanEqual(searchText, minPrice);
        // Spring Data JPA derives the query from the repository method name.
    }

    @Transactional(readOnly = true)
    public Product getProductByExactNameUsingJpql(String name) {
        return productRepository.findByNameUsingJpql(name)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with name: " + name));
        // This method exists to show a simple JPQL @Query repository method.
    }

    @Transactional(readOnly = true)
    public List<Product> getExpensiveProductsNative(double minPrice) {
        return productRepository.findExpensiveProductsNative(minPrice);
        // This method exists to show a simple native SQL @Query repository method.
    }

    @Transactional(readOnly = true)
    public List<ProductSummary> getProductSummariesByPrice(double minPrice) {
        return productRepository.findSummariesByPriceAtLeast(minPrice);
        // Projection query: returns selected fields instead of full Product entities.
    }

    @Transactional
    public Product createProduct(Product product) {
        product.setId(null);
        return productRepository.save(product);
        // null id + GenerationType.IDENTITY means PostgreSQL generates the primary key during INSERT.
    }

    @Transactional
    public Product updateProduct(Long id, Product product) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        existingProduct.setName(product.getName());
        existingProduct.setPrice(product.getPrice());
        // no need of explicit save here: save is for -- new entity to persist(),existing entity to merge()
        return existingProduct;
        // existingProduct is managed inside this transaction.
        // Hibernate dirty checking detects setter changes and issues UPDATE when the transaction flushes/commits.
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }

        productRepository.deleteById(id);
    }
}
