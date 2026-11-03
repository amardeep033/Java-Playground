package com.eventrelay.servicea.repository;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import com.eventrelay.servicea.entity.ApiKey;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Page<ApiKey> findByApplicationId(UUID applicationId, Pageable pageable);
    Optional<ApiKey> findByIdAndApplicationId(UUID id, UUID applicationId);
    Optional<ApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from ApiKey k where k.id = :id and k.applicationId = :applicationId")
    Optional<ApiKey> findForUpdate(UUID id, UUID applicationId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update ApiKey k set k.deletedAt = :at, k.revokedAt = coalesce(k.revokedAt, :at) where k.applicationId = :appId and k.deletedAt is null")
    int softDeleteByApplicationId(UUID appId, java.time.Instant at);
}
