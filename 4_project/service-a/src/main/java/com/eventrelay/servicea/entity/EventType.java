package com.eventrelay.servicea.entity;
import java.util.*;
import jakarta.persistence.*;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@org.hibernate.annotations.SQLDelete(sql = "UPDATE service_a.event_types SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Entity
@Table(name = "event_types")
public class EventType extends BaseEntity {
    @Column(name="is_active", nullable=false)
    private boolean active=true;
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active=active; }

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", nullable = true, length = 500)
    private String description;
    
    protected EventType() {}
    public EventType(UUID applicationId, String name, String description) {
        this.applicationId = applicationId;
        this.name = name;
        this.description = description;
    }
    public UUID getApplicationId() { return applicationId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public void updateDescription(String description) { this.description=description; }
}
