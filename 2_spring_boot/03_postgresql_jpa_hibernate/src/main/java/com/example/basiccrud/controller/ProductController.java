package com.example.basiccrud.controller;

import com.example.basiccrud.dto.ProductRequest;
import com.example.basiccrud.dto.ProductResponse;
import com.example.basiccrud.dto.ProductSummary;
import com.example.basiccrud.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Same API boundary as 02: controller still accepts ProductRequest and returns ProductResponse.
// The controller does not know that storage moved from memory to PostgreSQL.
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return ProductResponse.from(productService.getProductById(id));
    }

    @GetMapping("/search")
    public List<ProductResponse> searchProducts(
            @RequestParam(required = false, defaultValue = "") String name,
            @RequestParam(required = false, defaultValue = "0.0") double minPrice) {
        return productService.searchProducts(name, minPrice).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/jpql")
    public ProductResponse getProductByExactNameUsingJpql(@RequestParam String name) {
        return ProductResponse.from(productService.getProductByExactNameUsingJpql(name));
        // Calls a repository method with explicit JPQL: entity fields, not table columns.
    }

    @GetMapping("/native/expensive")
    public List<ProductResponse> getExpensiveProductsNative(
            @RequestParam(required = false, defaultValue = "0.0") double minPrice) {
        return productService.getExpensiveProductsNative(minPrice).stream()
                .map(ProductResponse::from)
                .toList();
        // Calls a native SQL query. Useful when PostgreSQL-specific SQL is really needed.
    }

    @GetMapping("/summaries")
    public List<ProductSummary> getProductSummaries(
            @RequestParam(required = false, defaultValue = "0.0") double minPrice) {
        return productService.getProductSummariesByPrice(minPrice);
        // Projection endpoint: returns only the fields selected by the query.
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse createdProduct = ProductResponse.from(productService.createProduct(request.toProduct()));
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(productService.updateProduct(id, request.toProduct()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
