package com.eventrelay.servicea.apikey;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID>, JpaSpecificationExecutor<ApiKey> {
    Page<ApiKey> findByApplicationId(UUID app_id, Pageable pageable);

    Optional<ApiKey> findByIdAndApplicationId(UUID api_key_id, UUID app_id);

    Optional<ApiKey> findByKeyHashAndRevokedAtIsNull(String key_hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from ApiKey k where k.id = :id and k.applicationId = :applicationId")
    Optional<ApiKey> findForUpdate(@org.springframework.data.repository.query.Param("id") UUID api_key_id, @org.springframework.data.repository.query.Param("applicationId") UUID app_id);

    @Modifying
    @Query("update ApiKey k set k.deletedAt = :at, k.revokedAt = coalesce(k.revokedAt, :at) where k.applicationId = :appId and k.deletedAt is null")
    int softDeleteByApplicationId(@org.springframework.data.repository.query.Param("appId") UUID app_id, Instant at);
}
