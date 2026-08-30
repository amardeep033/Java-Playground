package com.example.basiccrud.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders_05_checkout")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    // idempotencyKey is unique because the same client retry must map to one logical order.
    // The service checks for an existing key first on the normal path.
    // The database unique constraint is the final safeguard if two duplicate requests race to insert.
    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private String customerEmail;

    @Column
    private String sku;

    @Column
    private int quantity;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Instant createdAt;

    public CustomerOrder() {
    }

    public CustomerOrder(
            String orderNumber,
            String idempotencyKey,
            String customerEmail,
            String sku,
            int quantity,
            BigDecimal totalAmount) {
        this.orderNumber = orderNumber;
        this.idempotencyKey = idempotencyKey;
        this.customerEmail = customerEmail;
        this.sku = sku;
        this.quantity = quantity;
        this.status = "PENDING";
        this.totalAmount = totalAmount;
        this.createdAt = Instant.now();
    }

    public void markSuccess() {
        this.status = "SUCCESS";
    }

    public void markFailed() {
        this.status = "FAILED";
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
