package com.example.basiccrud.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity // Marks this class as a JPA entity.
@Table(name = "products") // Explicit table name avoids relying only on default naming rules.
public class Product {

    @Id // Primary key column.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PostgreSQL generates the id when a row is inserted.
    private Long id;

    // This is JPA mapping metadata, not request validation.
    // With ddl-auto=update, Hibernate uses it to create a NOT NULL database column.
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double price;

    // JPA requires an empty constructor so Hibernate can create entity objects when reading database rows.
    public Product() {
    }

    public Product(Long id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    // Getters do not build SQL by themselves here.
    // They just read values from this Product object after Hibernate has already fetched or saved it.
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


// PostgreSQL
//      ↓
// findById()
//      ↓
// Hibernate
//      ↓
// Persistence Context: set of managed entity instances associated with an EntityManager(cache: special in-memory workspace)
//      ↓
// Product object: managed entity - after the entity is loaded inside an active persistence context, Hibernate is tracking that entity

// Order a = entityManager.find(Order.class, 1L);
// Order b = entityManager.find(Order.class, 1L);
// a == b
// This is the first-level cache / identity map behavior.


//       transient: new() - Java object exists, Database doesn't know about it
//           ↓
//       use persist() 
//           ↓
//        managed  : hibernate starts tracking
//       ↙       ↘
//  detached     removed for delete


// Persistence context quick flow:
// Product row in PostgreSQL
//     -> repository.findById()
//     -> Hibernate loads the row
//     -> Product object becomes managed inside the active persistence context

// Persistence context = Hibernate's in-memory tracking workspace for one unit of work.
// In normal Spring Boot code, that unit of work is usually a @Transactional service method.

// Dirty checking:
// If existingProduct is managed inside @Transactional, then existingProduct.setPrice(...)
// marks the entity dirty. Hibernate can issue UPDATE during flush/commit even without save().

// Then why does save() exist?
// - new Product(...) is transient. Hibernate is not tracking it yet.
// - repository.save(newProduct) makes Spring Data JPA call EntityManager.persist(...) for a new entity.
// - repository.save(existingOrDetachedProduct) usually uses merge-style behavior for an existing entity.
// - merge() copies the state of the detached entity into a managed entity and returns the managed instance: These are potentially two different Java objects.

// Do I always need save() after changing an entity?
// No, if the entity is already managed inside @Transactional.
// Yes, for a new entity. Usually yes for a detached entity unless you load the managed entity first.

// persist vs save:
// - persist is the lower-level JPA operation from EntityManager.
// - save is the Spring Data repository method we normally use in this project.
// - For learning: save(new Product(...)) eventually means "start tracking this new entity and INSERT it".

// persist vs merge:
// persists leads Transient entity to Managed entity
// merge leads detached entity to Managed copy: Hibernate copies the state from the detached object into the managed instance. so two different obj
// so start using managed entity from this point -- also since id is already there in managed so just copies state back

// Flush vs commit:
// SQL does not always execute on the setter line.
// Hibernate can delay INSERT/UPDATE/DELETE until flush, and flush normally happens before commit.
// Flush synchronizes; commit makes the transaction durable.