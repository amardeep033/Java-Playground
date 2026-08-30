package com.example.basiccrud.exception;

// Same custom exception role as 02: service throws this when a requested product does not exist.
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Product not found with id: " + id);
    }

    public ProductNotFoundException(String message) {
        super(message);
    }
}
