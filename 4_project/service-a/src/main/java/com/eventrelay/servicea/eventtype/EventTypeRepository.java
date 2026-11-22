package com.eventrelay.servicea.eventtype;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EventTypeRepository extends JpaRepository<EventType, UUID>, JpaSpecificationExecutor<EventType> {
    Optional<EventType> findByApplicationIdAndName(UUID app_id, String name);

    Page<EventType> findByApplicationId(UUID app_id, Pageable pageable);

    Optional<EventType> findByIdAndApplicationId(UUID event_type_id, UUID app_id);

    @Modifying
    @Query("update EventType k set k.deletedAt = :at where k.applicationId = :appId and k.deletedAt is null")
    int softDeleteByApplicationId(@org.springframework.data.repository.query.Param("appId") UUID app_id, Instant at);
}
