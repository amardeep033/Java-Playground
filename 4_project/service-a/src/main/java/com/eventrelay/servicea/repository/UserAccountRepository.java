package com.eventrelay.servicea.repository;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.eventrelay.servicea.entity.UserAccount;
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from UserAccount u where u.id = :id")
    Optional<UserAccount> findForUpdate(UUID id);
}
