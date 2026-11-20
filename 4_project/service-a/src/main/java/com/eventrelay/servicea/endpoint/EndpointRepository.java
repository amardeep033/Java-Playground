package com.eventrelay.servicea.endpoint;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EndpointRepository extends JpaRepository<Endpoint, UUID>, JpaSpecificationExecutor<Endpoint> {
    @Query("select distinct e from Endpoint e join e.eventTypes t where e.applicationId = :appId and t.id = :eventTypeId and e.enabled = true order by e.id")
    List<Endpoint> findSubscribed(UUID appId, UUID eventTypeId);

    @Query("select distinct e.id from Endpoint e join e.eventTypes t where e.applicationId=:appId and t.id=:eventTypeId and e.enabled=false order by e.id")
    List<UUID> findInactiveSubscribed(UUID appId, UUID eventTypeId);

    Page<Endpoint> findByApplicationId(UUID applicationId, Pageable pageable);

    Optional<Endpoint> findByIdAndApplicationId(UUID id, UUID applicationId);

    @Modifying
    @Query("update Endpoint k set k.deletedAt = :at where k.applicationId = :appId and k.deletedAt is null")
    int softDeleteByApplicationId(UUID appId, Instant at);
}
