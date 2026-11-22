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
    List<Endpoint> findSubscribed(@org.springframework.data.repository.query.Param("appId") UUID app_id, @org.springframework.data.repository.query.Param("eventTypeId") UUID event_type_id);

    @Query("select distinct e.id from Endpoint e join e.eventTypes t where e.applicationId=:appId and t.id=:eventTypeId and e.enabled=false order by e.id")
    List<UUID> findInactiveSubscribed(@org.springframework.data.repository.query.Param("appId") UUID app_id, @org.springframework.data.repository.query.Param("eventTypeId") UUID event_type_id);

    Page<Endpoint> findByApplicationId(UUID app_id, Pageable pageable);

    Optional<Endpoint> findByIdAndApplicationId(UUID endpoint_id, UUID app_id);

    @Modifying
    @Query("update Endpoint k set k.deletedAt = :at where k.applicationId = :appId and k.deletedAt is null")
    int softDeleteByApplicationId(@org.springframework.data.repository.query.Param("appId") UUID app_id, Instant at);
}
