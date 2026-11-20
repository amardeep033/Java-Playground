package com.eventrelay.servicea.apikey;

import com.eventrelay.servicea.common.persistence.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE service_a.api_keys SET deleted_at = CURRENT_TIMESTAMP, revoked_at = COALESCE(revoked_at, CURRENT_TIMESTAMP) WHERE id = ?")
@Entity
@Table(name = "api_keys")
public class ApiKey extends BaseEntity {

    // base: id createdAt deletedAt
    // here: name keyhash keyprefix revokedat replacedby
    // rel: appid

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

    //--------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void revoke() {
        if (revokedAt == null)
            revokedAt = Instant.now();
    }

    public UUID getReplacedById() {
        return replacedById;
    }

    public void replaceWith(UUID replacementId) {
        revoke();
        replacedById = replacementId;
    }

    //----------------------------------------------------------

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    public UUID getApplicationId() {
        return applicationId;
    }

    //-----------------------------------------------------------

    protected ApiKey() {
    }

    public ApiKey(UUID applicationId, String name, String keyHash, String keyPrefix) {
        this.applicationId = applicationId;
        this.name = name;
        this.keyHash = keyHash;
        this.keyPrefix = keyPrefix;
    }

}
