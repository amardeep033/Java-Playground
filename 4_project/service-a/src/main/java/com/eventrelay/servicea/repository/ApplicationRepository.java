package com.eventrelay.servicea.repository;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.eventrelay.servicea.entity.Application;
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    Page<Application> findByUserId(UUID userId, Pageable pageable);
    Optional<Application> findByIdAndUserId(UUID id, UUID userId);
    List<Application> findAllByUserId(UUID userId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Application a where a.id = :id")
    Optional<Application> findForUpdate(UUID id);
}
