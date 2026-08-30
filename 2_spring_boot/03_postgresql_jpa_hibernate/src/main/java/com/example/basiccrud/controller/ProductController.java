package com.example.basiccrud.controller;

import com.example.basiccrud.dto.ProductRequest;
import com.example.basiccrud.dto.ProductResponse;
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

// Model to DTO conversion keeps internal Product separate from external ProductRequest/ProductResponse.
// Controller does not need try-catch because @RestControllerAdvice catches matching exceptions globally.

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

    // If service throws ProductNotFoundException, GlobalExceptionHandler returns 404 with ApiError.
    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return ProductResponse.from(productService.getProductById(id));
        // Same behavior as 02: service either returns Product or throws ProductNotFoundException.
        // This keeps the controller focused on success response conversion.
    }

    @GetMapping("/search")
    public List<ProductResponse> searchProducts(
            @RequestParam(required = false, defaultValue = "") String name,
            @RequestParam(required = false, defaultValue = "0.0") double minPrice) {
        return productService.searchProducts(name, minPrice).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse createdProduct = ProductResponse.from(productService.createProduct(request.toProduct()));
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    // No inline 404 handling here; missing id is handled globally through ProductNotFoundException.
    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(productService.updateProduct(id, request.toProduct()));
        // Same behavior as 02: update returns Product or throws ProductNotFoundException.
        // GlobalExceptionHandler converts that exception into the HTTP error response.
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
