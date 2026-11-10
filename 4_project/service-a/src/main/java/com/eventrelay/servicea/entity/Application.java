package com.eventrelay.servicea.entity;
import java.util.*;
import jakarta.persistence.*;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@org.hibernate.annotations.SQLDelete(sql = "UPDATE service_a.applications SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Entity
@Table(name = "applications")
public class Application extends BaseEntity {
    @Column(name="is_active", nullable=false)
    private boolean active=true;
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active=active; }

    @Column(name="client_id",nullable=false)
    private UUID clientId;
    public UUID getClientId() { return clientId; }
    public void assignClient(UUID id) { clientId=id; }
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;
    
    @Column(length=500)
    private String description;
    public String getDescription() { return description; }
    protected Application() {}
    public Application(UUID userId, String name) {
        this.userId = userId;
        this.name = name;
    }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public void updateMetadata(String name, String description) { this.name=name; this.description=description; }
}
