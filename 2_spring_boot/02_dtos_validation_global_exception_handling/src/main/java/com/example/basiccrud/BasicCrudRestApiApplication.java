package com.example.basiccrud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Product model vs ProductRequest/ProductResponse is needed to separate internal data from API input/output.
// Service returned Optional in 01, but now returns data or throws meaningful exceptions; controller is free from error formatting.
// Before, controller built ResponseEntity.notFound() inline; now ProductNotFoundException + GlobalExceptionHandler builds a consistent ApiError body.

@SpringBootApplication
public class BasicCrudRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicCrudRestApiApplication.class, args); // Creates the Spring context and starts embedded Tomcat.
    }
}
