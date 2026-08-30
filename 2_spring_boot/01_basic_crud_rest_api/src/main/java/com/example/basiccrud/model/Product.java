package com.example.basiccrud.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public class Product {

    private Long id;

    @NotBlank(message = "name is required")
    private String name;

    @PositiveOrZero(message = "price must be zero or positive")
    private double price;

    public Product() {
    }

    public Product(Long id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
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

// shortcut for writing above code:
// @Data -- provides getters, setters, toString(), equals(), and hashCode().
// @NoArgsConstructor -- provides the empty constructor required by JSON-to-object conversion.
// @AllArgsConstructor -- provides a constructor with all fields: id, name, and price.

// Validation annotations like @NotBlank and @PositiveOrZero protect the API from invalid request bodies.
// public class Product {
//     private Long id;
//     private String name;
//     private double price;
// }
