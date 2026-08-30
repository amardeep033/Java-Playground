package com.example.basiccrud.service;

import com.example.basiccrud.model.AuditLog;
import com.example.basiccrud.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // REQUIRES_NEW starts a separate transaction for the audit row.
    // If the main checkout transaction later rolls back, this audit row can still remain committed.
    // Useful for tracing attempts and failures, but use it carefully: the audit row can describe work that never committed.
    // Note: REQUIRES_NEW takes a SECOND DB connection while the caller still holds its own.
    // In real code audit either gets its own small pool or is written after commit. Otherwise, with the default pool of 10,
    // 10 concurrent checkouts each hold one connection and all block waiting for a second one, which deadlocks the pool.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String message) {
        auditLogRepository.save(new AuditLog(message));
        System.out.println("[AUDIT] " + message);
    }
}
