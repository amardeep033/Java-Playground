package com.example.basiccrud.dto;

// DTO projection used by a repository query when the API/use case needs only selected columns.
// This avoids loading/exposing the full Product entity when only name and price are needed.
public class ProductSummary {

    private String name;
    private double price;

    public ProductSummary(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}
