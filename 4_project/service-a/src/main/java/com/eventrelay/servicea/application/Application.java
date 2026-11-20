package com.eventrelay.servicea.application;

import com.eventrelay.servicea.common.persistence.BaseEntity;
import jakarta.persistence.*;
import java.util.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE service_a.applications SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Entity
@Table(name = "applications")
public class Application extends BaseEntity {

    // base: id createdAt deletedAt
    // here: name isActive desc
    // rel: userid? clientid

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    //--------------------------------------------------------------

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    // It groups related changes into one meaningful operation:
    public void updateMetadata(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    //--------------------------------------------------------------

    // The user recorded when creating the application.
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public UUID getUserId() {
        return userId;
    }

    //--------------------------------------------------------------

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    public UUID getClientId() {
        return clientId;
    }

    public void assignClient(UUID id) {
        clientId = id;
    }

    //----------------------------------------------------

    protected Application() {
    }

    public Application(UUID userId, String name) {
        this.userId = userId;
        this.name = name;
    }
}
