package com.eventrelay.servicea.entity;
import java.util.*;
import jakarta.persistence.*;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@org.hibernate.annotations.SQLDelete(sql = "UPDATE service_a.users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Entity
@Table(name = "users")
public class UserAccount extends BaseEntity {
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    
    @Column(name = "role", nullable = false, length = 16)
    private String role = "USER";

    public String getRole() { return role; }
    public void changeRole(String role) { this.role = role; }
    @Column(name="client_id",nullable=false)
    private UUID clientId;
    public UUID getClientId() { return clientId; }
    public void assignClient(UUID clientId) { this.clientId=clientId; }
    protected UserAccount() {}
    public UserAccount(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void changePasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void updateName(String name) { this.name=name; }
}
