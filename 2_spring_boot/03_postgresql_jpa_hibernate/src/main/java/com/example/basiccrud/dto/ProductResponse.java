package com.example.basiccrud.dto;

import com.example.basiccrud.model.Product;

// Same response DTO as 02: clients still receive id, name, and price.
// Returning a DTO keeps database/entity details out of the public API.
public class ProductResponse {

    private Long id;
    private String name;
    private double price;

    public ProductResponse() {
    }

    public ProductResponse(Long id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
