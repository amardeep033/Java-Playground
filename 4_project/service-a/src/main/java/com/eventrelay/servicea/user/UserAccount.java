package com.eventrelay.servicea.user;

import com.eventrelay.servicea.common.persistence.BaseEntity;
import jakarta.persistence.*;
import java.util.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@SQLRestriction("deleted_at IS NULL") //Tells Hibernate to add a SQL condition when loading this entity.
@SQLDelete(sql = "UPDATE service_a.users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?") //Tells Hibernate to execute your custom SQL instead of a normal entity DELETE.
@Entity
@Table(name = "users")
public class UserAccount extends BaseEntity {

    // base: id createdAt deletedAt
    // here: name email passwordHash role
    // rel: clientid

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "role", nullable = false, length = 16)
    private String role = "USER";

    //--------------------------------------------------------------

    public String getName() {
        return name;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void changeRole(String role) {
        this.role = role;
    }

    //--------------------------------------------------------------

    // @Column maps that UUID directly to the database column. The database can still enforce a foreign key
    // a JPA relationship annotation is not required for just foreign key relation, needed when want to store whole client entity object
    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    public UUID getClientId() {
        return clientId;
    }

    public void assignClient(UUID clientId) {
        this.clientId = clientId;
    }

    //--------------------------------------------------------------

    protected UserAccount() {
    }

    public UserAccount(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

}


// Alternative design; requires a Client entity
// @ManyToOne(fetch = FetchType.LAZY)
// @JoinColumn(name = "client_id", nullable = false)
// private Client client;
