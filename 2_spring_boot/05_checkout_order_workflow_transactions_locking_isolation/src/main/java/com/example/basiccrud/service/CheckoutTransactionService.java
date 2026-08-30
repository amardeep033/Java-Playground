package com.example.basiccrud.service;

import com.example.basiccrud.model.CustomerOrder;
import com.example.basiccrud.model.OutboxEvent;
import com.example.basiccrud.model.ProductInventory;
import com.example.basiccrud.repository.OrderRepository;
import com.example.basiccrud.repository.OutboxEventRepository;
import com.example.basiccrud.repository.ProductInventoryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

// @Transactional works through a Spring proxy.
// A call from CheckoutService -> CheckoutTransactionService crosses that proxy, so Spring can start/commit/rollback.
// If CheckoutService called its own @Transactional helper method, that self-invocation could bypass the proxy.
// Separate service keeps transaction boundaries explicit and prevents that common surprise.

@Service
public class CheckoutTransactionService {

    private final ProductInventoryRepository productRepository;
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public CheckoutTransactionService(
            ProductInventoryRepository productRepository,
            OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public CheckoutService.CheckoutResult findExistingResult(String idempotencyKey) {
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(CheckoutService.CheckoutResult::from)
                .orElse(null);
    }

    // Inventory reservation model:
    // This sample directly reduces availableQuantity to keep the code small.
    //
    // A more production-like model often separates:
    // | Bucket | Meaning |
    // | ------ | ------- |
    // | available stock | Can still be purchased. |
    // | reserved stock | Temporarily held for an order/payment attempt. |
    //
    // Reservation rows usually have an expiry time.
    // If payment never completes, a cleanup job can release expired reservations.
    // If stock runs out, domain events can trigger alerts such as an "inventory empty" email.
    @Transactional
    public CheckoutService.PendingOrder createPendingOrderAndReserveStock(
            String orderNumber,
            String idempotencyKey,
            String customerEmail,
            String sku,
            int quantity) {

        // This must be recorded irrespective of any later failure, so it runs in its own transaction (REQUIRES_NEW).
        auditService.log("Checkout started for key " + idempotencyKey);

        ProductInventory product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Product not found: " + sku));

        // Transaction (atomicity) ≠ serialization (concurrency). @Transactional only promises this block commits or rolls back as one unit. It says nothing about other transactions running at the same moment; that is the isolation level's job, and locks are how the DB enforces it.
        // Why row-level locking alone is not enough:
        // - A write locks the rows it touches, so it can only protect rows that already exist. "No row matched" is not something you can lock.
        // - So two transactions can both pass the same WHERE check and both insert, or a range you already read can gain new rows before you commit (phantom read).
        // - Preventing that needs a range lock (gap / next-key lock), and the DB takes those on an index. The index defines the range, so the index decides what gets locked.
        // - With no suitable index the DB cannot lock a narrow range, so it locks every row it scans instead, which approaches locking the whole table. Still correct, but throughput collapses and deadlocks show up.
        // Takeaway: indexes here are not only a speed decision, they set lock granularity. Same reason the unique constraints on orderNumber/idempotencyKey matter - they are enforced by an index, which is what makes a duplicate insert fail cleanly instead of racing.
        // Stock concurrency strategy used in code: atomic update.
        //
        // What can go wrong:
        // Two users may try to buy the last item at the same time.
        // Do not split it into read stock -> check in Java -> update stock.
        //
        // Why atomic update works here:
        // The DB checks availableQuantity >= quantity and reduces stock in one SQL statement.
        // updatedRows = 1 means success.
        // updatedRows = 0 means missing/insufficient stock or another transaction already took it.
        //
        // Other valid strategies to discuss in interviews:
        // | Strategy | When Useful |
        // | -------- | ----------- |
        // | Optimistic locking | Add version column; fail/retry when concurrent update is detected. |
        // | Pessimistic locking | Use row lock when conflict is expected and a longer decision needs protected data. |
        // | Higher isolation | Use when transaction visibility rules matter; may reduce throughput. |
        // | Deadlock retry | Even correct code can deadlock; keep transactions short and retry deadlock errors. |

        int updatedRows = productRepository.reserveStock(sku, quantity);
        if (updatedRows != 1) {
            throw new ResponseStatusException(BAD_REQUEST, "Insufficient stock for sku: " + sku);
        }

        CustomerOrder order = new CustomerOrder(
                orderNumber,
                idempotencyKey,
                customerEmail,
                sku,
                quantity,
                product.getPrice().multiply(java.math.BigDecimal.valueOf(quantity)));
        CustomerOrder savedOrder = orderRepository.save(order);

        // Outbox row is written in the same DB transaction as the order.
        // If this transaction rolls back, both order and event disappear together.
        outboxEventRepository.save(new OutboxEvent(
                "ORDER_PENDING",
                savedOrder.getOrderNumber(),
                "Order reserved stock and is waiting for payment"));

        // TransactionalEventListener is only for local after-commit work.
        // It is not a durable cross-service event mechanism.
        eventPublisher.publishEvent(new OrderCreatedLocalEvent(savedOrder.getOrderNumber()));

        return new CheckoutService.PendingOrder(savedOrder.getOrderNumber(), savedOrder.getTotalAmount());
    }

    @Transactional
    public CheckoutService.CheckoutResult markSuccess(String orderNumber) {
        CustomerOrder order = findOrder(orderNumber);
        order.markSuccess();

        outboxEventRepository.save(new OutboxEvent(
                "ORDER_SUCCESS",
                order.getOrderNumber(),
                "Payment succeeded; downstream services can react"));

        auditService.log("Order succeeded " + order.getOrderNumber());
        return CheckoutService.CheckoutResult.from(order);
    }

    @Transactional
    public void markFailedAndQueueCompensation(String orderNumber) {
        CustomerOrder order = findOrder(orderNumber);
        order.markFailed();

        outboxEventRepository.save(new OutboxEvent(
                "ORDER_FAILED",
                order.getOrderNumber(),
                "Payment failed; compensation worker should release inventory or refund if needed"));

        // Note: in real code this also puts the reserved quantity back (and refunds if the charge went through).
        // Writing only the outbox row means every failed payment permanently leaks that stock.
        System.out.println("[COMPENSATION PLACEHOLDER] release stock/refund for " + orderNumber);
    }

    private CustomerOrder findOrder(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found."));
    }
}
