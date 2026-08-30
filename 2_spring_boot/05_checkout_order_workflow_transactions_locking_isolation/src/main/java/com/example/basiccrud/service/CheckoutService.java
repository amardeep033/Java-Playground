package com.example.basiccrud.service;

import com.example.basiccrud.fakeexternalservice.FakePaymentGateway;
import com.example.basiccrud.model.CustomerOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

// By default, Spring rolls back on unchecked exceptions.
// For checked business exceptions, use rollbackFor when needed.
// Rollback only undoes database work in the current transaction.
// It cannot unsend an email, unpublish a message, or undo a payment provider call.
// For already-started external side effects, use compensation: refund payment, release stock, or queue a repair event.
//
// Keep transactions small:
// 1. local DB work: reserve stock + create PENDING order
// 2. external payment call outside DB transaction
// 3. local DB work: mark SUCCESS or FAILED
//
// Reason: each transaction should protect one logical database unit of work.
// Holding DB locks while waiting for payment/network increases latency, lock contention, and deadlock risk.

@Service
public class CheckoutService {

    private final CheckoutTransactionService transactionService;
    private final FakePaymentGateway paymentGateway;

    public CheckoutService(
            CheckoutTransactionService transactionService,
            FakePaymentGateway paymentGateway) {
        this.transactionService = transactionService;
        this.paymentGateway = paymentGateway;
    }

    public CheckoutResult checkout(String idempotencyKey, String customerEmail, String sku, int quantity) {
        String normalizedKey = normalize(idempotencyKey);
        String normalizedEmail = normalize(customerEmail);
        String normalizedSku = normalize(sku);

        // Note: this read and the insert in step 2 are not one atomic step, so two concurrent duplicates can both get past it.
        // The unique constraint on idempotencyKey stops the second insert, but in real code that violation is caught,
        // re-read, and returned as the existing order. Uncaught, it surfaces as a 500 on what is a legitimate retry.
        // 1. Idempotency check comes first.
        // If the client retries the same checkout, return the same result instead of creating a second order.
        CheckoutResult existingOrder = transactionService.findExistingResult(normalizedKey);
        if (existingOrder != null) {
            System.out.println("[IDEMPOTENCY] duplicate request; returning existing order " + existingOrder.orderNumber());
            return existingOrder;
        }

        // 2. Small DB transaction.
        // Create PENDING order, reserve stock, write outbox row, write audit row.
        PendingOrder pendingOrder = transactionService.createPendingOrderAndReserveStock(
                nextOrderNumber(),
                normalizedKey,
                normalizedEmail,
                normalizedSku,
                quantity);

        // 3. External service call outside DB transaction.
        // Do not hold DB locks while waiting for payment provider/network.
        boolean paymentSuccess = paymentGateway.charge(
                pendingOrder.orderNumber(),
                normalizedKey,
                pendingOrder.totalAmount());

        // Payment vs inventory-reservation race:
        // In real systems, stock reservation may expire after N minutes.
        // Payment may return after that expiry window.
        //
        // confirmOrder/markSuccess must enforce the business rule:
        // - if the reservation is still valid, mark SUCCESS
        // - if the reservation expired, do not complete the order blindly
        // - compensate instead, usually refund payment and release/repair inventory state
        //
        // This sample keeps expiry as a comment because the chapter focuses on transaction boundaries.
        if (paymentSuccess) {
            // Note: in real code this call is wrapped in retry. Payment is already captured at this point, so if markSuccess
            // throws, the money is taken and the order is stuck in PENDING. Retry alone is not enough either: the process can
            // die here, so a startup/scheduled sweeper over old PENDING orders is what actually closes this hole.
            // 4. Small DB transaction.
            // Mark order SUCCESS and write a second outbox event.
            return transactionService.markSuccess(pendingOrder.orderNumber());
        }

        // 5. Compensation placeholder.
        // Refund/release logic is not implemented; this only marks where it belongs.
        transactionService.markFailedAndQueueCompensation(pendingOrder.orderNumber());
        throw new ResponseStatusException(BAD_REQUEST, "Payment failed after retry.");
    }

    // Note: uppercasing everything is wrong outside a demo. Only sku wants it. Email should keep its case, and an
    // idempotency key must never be case-folded - folding makes "abc" and "ABC" the same key, so one client's retry
    // could return another client's order.
    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Required value is blank.");
        }
        return value.trim().toUpperCase();
    }

    private String nextOrderNumber() {
        return "ORD-05-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public record PendingOrder(String orderNumber, BigDecimal totalAmount) {
    }

    public record CheckoutResult(
            String orderNumber,
            String idempotencyKey,
            String status,
            String sku,
            int quantity,
            BigDecimal totalAmount,
            Instant createdAt) {

        public static CheckoutResult from(CustomerOrder order) {
            return new CheckoutResult(
                    order.getOrderNumber(),
                    order.getIdempotencyKey(),
                    order.getStatus(),
                    order.getSku(),
                    order.getQuantity(),
                    order.getTotalAmount(),
                    order.getCreatedAt());
        }
    }
}
