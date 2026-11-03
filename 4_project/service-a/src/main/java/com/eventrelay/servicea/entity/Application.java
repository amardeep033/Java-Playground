package com.eventrelay.servicea.entity;
import java.util.*;
import jakarta.persistence.*;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@org.hibernate.annotations.SQLDelete(sql = "UPDATE service_a.applications SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Entity
@Table(name = "applications")
public class Application extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;
    
    protected Application() {}
    public Application(UUID userId, String name) {
        this.userId = userId;
        this.name = name;
    }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
}
