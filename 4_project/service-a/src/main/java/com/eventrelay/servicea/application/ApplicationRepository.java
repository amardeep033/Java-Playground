package com.eventrelay.servicea.application;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ApplicationRepository extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {
    Page<Application> findByUserId(UUID user_id, Pageable pageable);

    Optional<Application> findByIdAndUserId(UUID app_id, UUID user_id);

    List<Application> findAllByClientId(UUID client_id);

    List<Application> findAllByUserId(UUID user_id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a where a.id = :id")
    Optional<Application> findForUpdate(@org.springframework.data.repository.query.Param("id") UUID app_id);
}
