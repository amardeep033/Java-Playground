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

// JpaRepository built-in methods like findById(), save(), and saveAndFlush() or Derived query like findByKeyHashAndRevokedAtIsNull(...) -- entity
// JPQL @Query -- no entity for these -- directly hardcoded as in outbox.java
