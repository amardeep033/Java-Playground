package com.eventrelay.servicea.application;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ApplicationRepository extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {
    Page<Application> findByUserId(UUID userId, Pageable pageable);

    Optional<Application> findByIdAndUserId(UUID id, UUID userId);

    List<Application> findAllByClientId(UUID clientId);

    List<Application> findAllByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a where a.id = :id")
    Optional<Application> findForUpdate(UUID id);
}
