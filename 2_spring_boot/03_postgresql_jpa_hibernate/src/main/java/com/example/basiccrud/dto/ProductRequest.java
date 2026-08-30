package com.example.basiccrud.dto;

import com.example.basiccrud.model.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

// Same request DTO as 02: clients still send only name and price.
// JPA entity annotations do not belong here because this class is API input, not database mapping.
public class ProductRequest {

    @NotBlank(message = "name is required")
    private String name;

    @PositiveOrZero(message = "price must be zero or positive")
    private double price;

    public ProductRequest() {
    }

    public ProductRequest(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public Product toProduct() {
        return new Product(null, name, price);
    }

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
