package com.eventrelay.servicea.eventtype;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EventTypeRepository extends JpaRepository<EventType, UUID>, JpaSpecificationExecutor<EventType> {
    Optional<EventType> findByApplicationIdAndName(UUID applicationId, String name);

    Page<EventType> findByApplicationId(UUID applicationId, Pageable pageable);

    Optional<EventType> findByIdAndApplicationId(UUID id, UUID applicationId);

    @Modifying
    @Query("update EventType k set k.deletedAt = :at where k.applicationId = :appId and k.deletedAt is null")
    int softDeleteByApplicationId(UUID appId, Instant at);
}
