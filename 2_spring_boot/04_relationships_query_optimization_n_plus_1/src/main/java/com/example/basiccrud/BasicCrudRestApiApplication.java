package com.example.basiccrud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Customer <--one way-- CustomerOrder <--two way--> OrderItem

// customers_04_order :: id (PK) | name | email 
// orders_04_order :: id (PK) | order_number | status | customer_id(FK)
// order_items_04_order :: id (PK) | product_name | quantity | unit_price | order_id (FK)

// CustomerOrder.customer  -> @ManyToOne
// CustomerOrder.items     -> @OneToMany(mappedBy = "order")
// OrderItem.order         -> @ManyToOne

// fetching order -- can lead to n+1 for order items
// Fix options:
// - JOIN FETCH: JPQL query explicitly fetches orders + items.
// - @EntityGraph: repository method declares the fetch plan without custom JPQL.
// - @BatchSize: query orders first, then Hibernate groups lazy item queries using where order_id in (...).

@SpringBootApplication
public class BasicCrudRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicCrudRestApiApplication.class, args);
    }
}

// The table that stores the foreign key is the owning side.
// CustomerOrder owns customer_id.
// OrderItem owns order_id.

// JPA relationship defaults:
// | Relationship  | Default Fetch Type | Example             | Practical Note |
// | ------------- | ------------------ | ------------------- | -------------- |
// | @ManyToOne    | EAGER              | Order -> Customer   | Often changed to LAZY in production to avoid accidental overfetching. |
// | @OneToOne     | EAGER              | User -> AuthProfile | Add a UNIQUE constraint on the FK if the DB must enforce true one-to-one. |
// | @OneToMany    | LAZY               | Customer -> Orders  | Keep LAZY by default; fetch children only when the use case needs them. |
// | @ManyToMany   | LAZY               | Student -> Course   | Use a join table; if the relationship has fields, model it as an entity. |