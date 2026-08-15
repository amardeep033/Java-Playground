package com.example.overall.s9transactions.service;

import com.example.overall.s9transactions.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final OrderRepository orderRepository;

    public AuditService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // REQUIRES_NEW means this audit insert gets its own independent transaction.
    // The call must come through this separate Spring bean so Spring's transaction proxy can intercept it.
    // If the outer OrderService transaction later rolls back, this audit transaction can still stay committed.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAudit(String note) {
        System.out.println("A1. AuditService -> Repository");
        orderRepository.saveAudit(note);
    }
}
