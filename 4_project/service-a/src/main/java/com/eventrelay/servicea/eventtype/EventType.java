package com.eventrelay.servicea.eventtype;

import com.eventrelay.servicea.common.persistence.BaseEntity;
import jakarta.persistence.*;
import java.util.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE service_a.event_types SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Entity
@Table(name = "event_types")
public class EventType extends BaseEntity {

    // base: id createdAt deletedAt
    // here: name desc isActive
    // rel: appid

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", nullable = true, length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    //------------------------------------------------------

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
        
    public void updateDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    //--------------------------------------------------------

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    public UUID getApplicationId() {
        return applicationId;
    }

    //--------------------------------------------------------

    protected EventType() {
    }

    public EventType(UUID applicationId, String name, String description) {
        this.applicationId = applicationId;
        this.name = name;
        this.description = description;
    }
}
