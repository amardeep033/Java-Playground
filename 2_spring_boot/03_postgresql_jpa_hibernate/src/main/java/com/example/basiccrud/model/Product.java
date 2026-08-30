package com.example.basiccrud.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// In 02, Product was a plain internal model stored in a map.
// In 03, Product is a JPA entity, so Hibernate maps it to a PostgreSQL table row.
// Validation is still on ProductRequest because validation rules describe external API input.

@Entity // Marks this class as persistent; JPA/Hibernate manages objects of this type.
@Table(name = "products") // Explicit table name avoids relying only on default naming rules.
public class Product {

    @Id // Primary key column.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PostgreSQL generates the id when a row is inserted.
    private Long id;

    @Column(nullable = false) // Database-level rule: product name cannot be null.
    private String name;

    @Column(nullable = false) // Database-level rule: product price cannot be null.
    private double price;

    // JPA requires an empty constructor so Hibernate can create entity objects when reading database rows.
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
