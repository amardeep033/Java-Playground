package com.eventrelay.servicea.common.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

//BaseEntity exists to reuse common database fields and behavior across multiple JPA entities
@MappedSuperclass //This class is not an entity/table of its own, but its persistent fields and mappings should be inherited by entity subclasses.
public abstract class BaseEntity { //abstract over interface because it can provide instance fields and not only impl method
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) //other strategy: IDENTITY, SEQUENCE
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    //------------------------------------

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void softDelete() {
        if (deletedAt == null)
            deletedAt = Instant.now();
    }
}

// 1. With Hibernate (Spring Data JPA): requires @Entity - extends JpaRepository - (UserAccountRepository)
// 1.1 Built-in        -- findById(), save(), deleteById()
// 1.2 Derived         -- findByEmail(), findByNameAndStatus()
// 1.3 @Query JPQL     -- @Query("SELECT u FROM UserAccount u") [Entity → SQL]
// 1.4 @Query Native   -- @Query(value = "SELECT * FROM users", nativeQuery = true) [Direct SQL]

// 2. Without Hibernate (Spring JDBC): no @Entity required - @Repository, JdbcTemplate, ObjectMapper - precise control needed (IncomingRequestStore)
// 2.1 JdbcTemplate    -- db.query("SELECT * FROM users", rowMapper)
// 2.2 JdbcTemplate    -- db.update("INSERT INTO users ...", params)
