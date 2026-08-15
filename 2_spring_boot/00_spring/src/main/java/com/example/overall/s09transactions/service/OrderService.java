package com.example.overall.s9transactions.service;

import com.example.overall.s9transactions.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final AuditService auditService;

    public OrderService(OrderRepository orderRepository, AuditService auditService) {
        this.orderRepository = orderRepository;
        this.auditService = auditService;
    }

    // @Transactional wraps this public method with a transaction proxy.
    // Flow: caller -> proxy -> BEGIN -> execute service method -> COMMIT or ROLLBACK.
    // It belongs at the service layer because one use case may need many repository calls to succeed/fail together.

    // @Transactional(rollbackFor = Exception.class)
    @Transactional
    public void placeOrder() throws Exception {
        System.out.println("S1. Service -> Repository");
        
        // 0.1 Outer method uses propagation = REQUIRED by default.
        // This order insert belongs to the outer transaction started for placeOrder().
        orderRepository.saveOrder("Order1");

        // 0.2 AuditService.saveAudit uses propagation = REQUIRES_NEW.
        // Because it is another Spring bean, the call goes through its proxy and starts a separate audit transaction.
        auditService.saveAudit("Audit for Order1");

        // 1. Checked exception: no rollback by default. It often represents expected/recoverable conditions.
        // throw new Exception("checked exception");

        // 2. Unchecked exception: rollback by default. It usually represents programming/application failures.
        throw new RuntimeException("unchecked exception");
    }
}

// By default, @Transactional uses: propagation = Propagation.REQUIRED
// It means multiple normal repository calls inside placeOrder share the same transaction; each DB call does not create a new one.
// Also, if a caller already has an active transaction, placeOrder joins that existing transaction.

// We can override this using @Transactional(propagation = Propagation.REQUIRES_NEW).
// Example: an audit DB call may need to commit even if save_order later fails, so put it in another bean with REQUIRES_NEW.
// Important: REQUIRES_NEW must be reached through a Spring proxy. A same-class call like this.saveAudit() bypasses the proxy.
