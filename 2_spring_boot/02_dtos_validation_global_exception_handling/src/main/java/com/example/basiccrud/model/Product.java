package com.example.basiccrud.model;

// Product is the internal model used by service/repository, separate from request and response DTOs.
// Validation is not here now because validation rules belong to ProductRequest, the external API input.

public class Product {

    private Long id;
    private String name;
    private double price;

    //--------------

    // Empty constructor helps frameworks create objects; all-args constructor is useful for manual creation/seeding.
    public Product() {
    }

    public Product(Long id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    //--------------

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
