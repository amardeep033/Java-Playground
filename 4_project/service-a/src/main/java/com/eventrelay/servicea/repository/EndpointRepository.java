package com.eventrelay.servicea.repository;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.eventrelay.servicea.entity.Endpoint;
public interface EndpointRepository extends JpaRepository<Endpoint, UUID> {
    @org.springframework.data.jpa.repository.Query("select distinct e from Endpoint e join e.eventTypes t where e.applicationId = :appId and t.id = :eventTypeId and e.enabled = true order by e.id")
    List<Endpoint> findSubscribed(UUID appId, UUID eventTypeId);
    Page<Endpoint> findByApplicationId(UUID applicationId, Pageable pageable);
    Optional<Endpoint> findByIdAndApplicationId(UUID id, UUID applicationId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Endpoint k set k.deletedAt = :at where k.applicationId = :appId and k.deletedAt is null")
    int softDeleteByApplicationId(UUID appId, java.time.Instant at);
}
