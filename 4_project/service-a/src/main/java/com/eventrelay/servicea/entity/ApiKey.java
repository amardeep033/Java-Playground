package com.eventrelay.servicea.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@org.hibernate.annotations.SQLDelete(sql = "UPDATE service_a.api_keys SET deleted_at = CURRENT_TIMESTAMP, revoked_at = COALESCE(revoked_at, CURRENT_TIMESTAMP) WHERE id = ?")
@Entity
@Table(name = "api_keys")
public class ApiKey extends BaseEntity {
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;
    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "replaced_by_id")
    private UUID replacedById;

    protected ApiKey() {}
    public ApiKey(UUID applicationId, String name, String keyHash, String keyPrefix) {
        this.applicationId = applicationId;
        this.name = name;
        this.keyHash = keyHash;
        this.keyPrefix = keyPrefix;
    }
    public UUID getApplicationId() { return applicationId; }
    public String getName() { return name; }
    public String getKeyHash() { return keyHash; }
    public String getKeyPrefix() { return keyPrefix; }
    public Instant getRevokedAt() { return revokedAt; }
    public UUID getReplacedById() { return replacedById; }
    public void revoke() { if (revokedAt == null) revokedAt = Instant.now(); }
    public void replaceWith(UUID replacementId) {
        revoke();
        replacedById = replacementId;
    }
}
