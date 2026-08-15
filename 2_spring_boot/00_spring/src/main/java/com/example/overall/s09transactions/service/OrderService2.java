package com.example.overall.s9transactions.service;

import com.example.overall.s9transactions.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService2 {
    private final OrderRepository orderRepository;

    public OrderService2(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Copy of OrderService for one purpose: show self-invocation breaking REQUIRES_NEW.
    // Controller -> OrderService2 proxy starts the outer transaction for placeOrder().
    @Transactional
    public void placeOrder() {
        System.out.println("S2. Service2 -> Repository");
        orderRepository.saveOrder("Order2");
        this.saveAudit("Audit for Order2");
        throw new RuntimeException("unchecked exception after self-invoked audit");
    }

    // REQUIRES_NEW would create a new independent transaction only if this method call passed through the Spring proxy.
    // 1. The transaction interceptor exists on the proxy.
    // 2. Here, this method is called through this.saveAudit(...), so the proxy is bypassed.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAudit(String note) {
        System.out.println("A2. self-invoked audit method");
        orderRepository.saveAudit(note);
    }
}

// Here:
// 1. Transaction interceptor exists on the proxy.
// 2. this.saveAudit() is self-invocation.
// 3. Self-invocation bypasses the proxy.
// 4. Therefore REQUIRES_NEW is NOT applied.
// 5. saveAudit() runs inside the existing T1 transaction.

// caller
//   ↓
// OrderService2 proxy
//   ↓
// placeOrder()
//   ↓
// BEGIN T1
//   ↓
// orderRepository.saveOrder() → T1
//   ↓
// this.saveAudit()             ← self-invocation
//   ↓
// saveAudit() directly         ← proxy bypassed
//   ↓
// REQUIRES_NEW NOT applied
//   ↓
// orderRepository.saveAudit() → T1
//   ↓
// RuntimeException
//   ↓
// ROLLBACK T1

// The same fundamental problem can affect other proxy-based Spring features:
// @Transactional
// @Cacheable
// @Async
// other proxy-based Spring features
