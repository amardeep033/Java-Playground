package com.eventrelay.servicea.apikey;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID>, JpaSpecificationExecutor<ApiKey> {
    Page<ApiKey> findByApplicationId(UUID applicationId, Pageable pageable);

    Optional<ApiKey> findByIdAndApplicationId(UUID id, UUID applicationId);

    Optional<ApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from ApiKey k where k.id = :id and k.applicationId = :applicationId")
    Optional<ApiKey> findForUpdate(UUID id, UUID applicationId);

    @Modifying
    @Query("update ApiKey k set k.deletedAt = :at, k.revokedAt = coalesce(k.revokedAt, :at) where k.applicationId = :appId and k.deletedAt is null")
    int softDeleteByApplicationId(UUID appId, Instant at);
}
