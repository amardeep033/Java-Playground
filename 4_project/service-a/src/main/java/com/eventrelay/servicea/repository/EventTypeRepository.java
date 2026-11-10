package com.eventrelay.servicea.repository;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.eventrelay.servicea.entity.EventType;
public interface EventTypeRepository extends JpaRepository<EventType, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<EventType> {
    Optional<EventType> findByApplicationIdAndName(UUID applicationId, String name);
    Page<EventType> findByApplicationId(UUID applicationId, Pageable pageable);
    Optional<EventType> findByIdAndApplicationId(UUID id, UUID applicationId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update EventType k set k.deletedAt = :at where k.applicationId = :appId and k.deletedAt is null")
    int softDeleteByApplicationId(UUID appId, java.time.Instant at);
}
