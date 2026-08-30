package com.example.basiccrud.exception;

// Custom exception for a common business/API failure: requested product id does not exist.
// Extending RuntimeException lets service methods throw it without declaring throws, and Spring can handle it globally.
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Product not found with id: " + id);
    }
}
