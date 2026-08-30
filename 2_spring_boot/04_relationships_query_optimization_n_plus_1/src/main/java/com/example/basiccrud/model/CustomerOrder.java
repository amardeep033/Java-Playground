package com.example.basiccrud.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(
        name = "orders_04_order",
        indexes = {
                @Index(name = "idx_orders_04_order_customer_id", columnList = "customer_id"),
                @Index(name = "idx_orders_04_order_status", columnList = "status")
        })
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private String status;

    // Parent FK | ManyToOne | Default fetch type: EAGER | DB column: customer_id.
    // Here we intentionally keep the default EAGER behavior so "fetch orders" also loads customer.
    // In most production code, prefer LAZY for ManyToOne too, but this chapter uses the default for comparison.
    // @JoinColumn is optional if the default column name is fine. By default it references the target entity PK.
    @ManyToOne
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
    private Customer customer;

    // Child collection | OneToMany | Default fetch type: LAZY | DB FK lives in order_items_04_order.order_id.
    // mappedBy = "order" means OrderItem.order owns the FK column. Without mappedBy, JPA may create a join table.
    // cascade = ALL means saving/deleting CustomerOrder cascades to its OrderItem children.
    // orphanRemoval = true means removing an item from this list deletes that row from the DB.
    // @BatchSize groups lazy item loading: instead of 1 item query per order, Hibernate loads items for up to 10 orders using IN (...).
    // Changing OneToMany to EAGER is not a real N+1 fix; it can overfetch data for every order endpoint.
    // The important question is not only "lazy or eager?", but "what fetch plan does this query need?".
    @BatchSize(size = 10) // Comment this line to compare batched lazy loading vs true N+1.
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    //-------------------------------------------------

    public CustomerOrder() {
    }

    public CustomerOrder(Long id, String orderNumber, String status, Customer customer) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.status = status;
        this.customer = customer;
    }

    //-------------------------------------------------

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}

// Cascade and orphanRemoval:
// In this model, OrderItem is lifecycle-owned by CustomerOrder.
// If a new item is added to an order, saving the order should also save the item row.
// If an order is deleted, its item rows should also be deleted.

// | Setting | Meaning |
// | ------- | ------- |
// | CascadeType.PERSIST | Persist child items when the parent order is persisted. |
// | CascadeType.MERGE | Merge child items when the parent order is merged. |
// | CascadeType.REMOVE | Delete child items when the parent order is deleted. |
// | CascadeType.ALL | Apply all cascade operations above. |
// | orphanRemoval = true | Delete a child item when it is removed from order.items. |

// Use cascade from parent to dependent child only when the child truly belongs to the parent.
// Example: CustomerOrder -> OrderItem is OK.
// Do not cascade REMOVE from Order -> Customer, because deleting one order must not delete the customer.

// Cascade REMOVE is triggered by deleting the parent:
// Before:
// CustomerOrder #1
//      ├── OrderItem A
//      └── OrderItem B
// Action:
// orderRepository.delete(order)

// After:
// CustomerOrder #1 row deleted
// OrderItem A row deleted
// OrderItem B row deleted
// orphanRemoval is triggered by removing a child from the parent's collection:

// Before:
// CustomerOrder #1
//      ├── OrderItem A
//      └── OrderItem B
// Action:
// order.getItems().remove(OrderItem A)

// After:
// CustomerOrder #1 row still exists
// OrderItem A row deleted
// OrderItem B row still exists
