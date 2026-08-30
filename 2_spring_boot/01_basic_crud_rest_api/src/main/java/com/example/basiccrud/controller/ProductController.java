package com.example.basiccrud.controller;

import com.example.basiccrud.model.Product;
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


// Controller class for one resource group: all product-related API endpoints live here.
@RestController
@RequestMapping("/api/products") // Class-level base path; method mappings are added under this path. Duplicate mappings in another controller cause an ambiguous mapping startup error.
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // 1.1 GET http://localhost:8080/api/products - returns all products.
    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    // 1.2 GET http://localhost:8080/api/products/1 - returns one product by id, or 404 if missing.
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 1.3 GET http://localhost:8080/api/products/search?name=pen&minPrice=5 - filters using optional query parameters with default values.
    @GetMapping("/search")
    public List<Product> searchProducts(
            @RequestParam(required = false, defaultValue = "") String name,
            @RequestParam(required = false, defaultValue = "0.0") double minPrice) {
        return productService.searchProducts(name, minPrice);
    }

    // 2 POST http://localhost:8080/api/products - creates a new product from the JSON request body.
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    // 3 PUT http://localhost:8080/api/products/1 - replaces the product with id 1 if it exists.
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody Product product) {
        return productService.updateProduct(id, product)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4 DELETE http://localhost:8080/api/products/1 - deletes the product with id 1 if it exists.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (!productService.deleteProduct(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}

// ResponseEntity: lets the controller return both the response body and the HTTP status code.
// notFound(): builds a 404 Not Found response when the requested resource does not exist.
// status(...): starts a response with a custom status, such as 201 Created for POST.
// noContent(): builds a 204 No Content response, usually after a successful DELETE.

// PathVariable reads values from the URL path like /api/products/1.
// Use PathVariable when the value identifies a specific resource, such as product id, user id, or order id.

// RequestParam reads query values like /api/products?id=1.
// Use RequestParam for optional inputs like filtering, searching, sorting, and pagination.

// RequestBody converts the JSON request body into a Java object using Spring's message converters.
// Valid asks Spring to check validation annotations like @NotBlank before the controller continues.
