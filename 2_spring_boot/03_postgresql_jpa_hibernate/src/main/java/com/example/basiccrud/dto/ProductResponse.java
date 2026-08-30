package com.example.basiccrud.dto;

import com.example.basiccrud.model.Product;

// Response DTO: fields the API sends back to the client.
public class ProductResponse {

    private Long id;
    private String name;
    private double price;

    // Empty constructor helps JSON conversion; all-args constructor is useful when building responses.
    public ProductResponse() {
    }

    public ProductResponse(Long id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    // Converts internal Product model into the external response shape.
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice());
    }

    // If an endpoint needs fewer fields, create another response DTO such as ProductSummaryResponse.

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
