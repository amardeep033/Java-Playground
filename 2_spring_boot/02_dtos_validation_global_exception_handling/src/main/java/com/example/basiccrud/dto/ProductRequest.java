package com.example.basiccrud.dto;

import com.example.basiccrud.model.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

// Request DTO: fields the client is allowed to send when creating/updating a product.
// Validation belongs here because this class represents external API input.

public class ProductRequest {

    @NotBlank(message = "name is required")
    private String name;

    @PositiveOrZero(message = "price must be zero or positive")
    private double price;

    //----------------------

    // Empty constructor helps JSON conversion; all-args constructor is useful for tests/manual creation.
    public ProductRequest() {
    }

    public ProductRequest(String name, double price) {
        this.name = name;
        this.price = price;
    }

    //----------------------

    // Converts API input DTO into the internal model used by service/repository.
    public Product toProduct() {
        return new Product(null, name, price);
    }

    //----------------------

    // ProductRequest has no id because create gets id from server and update gets id from the URL path.

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
