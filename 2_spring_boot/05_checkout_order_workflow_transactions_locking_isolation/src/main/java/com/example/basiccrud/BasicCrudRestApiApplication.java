package com.example.basiccrud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// NOTE: Assume every operation can happen twice, concurrently, or in the wrong order.
// | Duplicate checkout request | Idempotency key | A: CheckoutService checks idempotencyKey first. |
// | Concurrent stock update | Atomic update / locking concept | B: ProductInventoryRepository.reserveStock(). |
// | DB commit + event publish mismatch | Outbox pattern | C: Save OutboxEvent in same DB transaction as order. |
// | Audit should survive rollback | REQUIRES_NEW | D: AuditService.log() commits audit in separate transaction. |
// | Local cache/email only after commit | TransactionalEventListener | E: LocalOrderEventListener runs after successful commit. |
// | Payment timeout | Retry with backoff + provider idempotency key | F: FakePaymentGateway.charge() retries retryable failures. |
// | Payment succeeds but server crashes | Durable PENDING state + retry/idempotency | Restart can inspect PENDING orders and safely retry confirmation/compensation. |
// | Payment succeeds but confirmation fails | Retry then compensate | G1/G2: retry mark SUCCESS; if not repairable, mark FAILED and queue compensation. |
// | Analytics/email/warehouse updates | Event-driven async integration | Future OutboxPublisher publishes durable events to broker. |
// | Deadlock risk in larger workflows | Short transactions + deterministic lock order + retry | This example updates one stock row; bigger flows must order locks. |

// Why layers AND state: each row above makes one step correct, nothing more. A checkout is two DB transactions with a payment call in between, so no single layer covers the gaps between them.
// State closes the gaps: the order row records how far the workflow got. A duplicate request (A) is answered from it, and a restart can tell what still needs retrying (F) or compensating (G2).
// Transitions are one-way: PENDING -> SUCCESS or PENDING -> FAILED. Terminal is final, so a late or duplicate event cannot reopen a finished order.

@SpringBootApplication
public class BasicCrudRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicCrudRestApiApplication.class, args);
    }
}

// Services: CheckoutService (with AuditService and the local after-commit listener), payment, outbox publisher, and later analytics, email, and so on.

// Client sends API call: POST /api/checkout
//        ↓
// Controller receives HTTP request and passes DTO to service
//        ↓
// A:: {0. CheckoutService} Service checks idempotencyKey
//        ↓
// If same key already exists: return existing order response
//        ↓
// If new key:
//        ↓
// DB transaction starts
//        ↓
// B:: Reserve stock with atomic update
//        ↓
// Create order with state [STATE: PENDING]
//        ↓
// C:: {1. OutboxPattern} Write outbox row for durable cross-service event
//        ↓
// D:: {2. AuditService} Write audit row using REQUIRES_NEW, so audit can commit independently
//        ↓
// E:: {3. TransactionalEventListener} Publish local Spring event; TransactionalEventListener runs only after commit
//        ↓
// DB transaction commits
//        ↓
// F:: {4. PaymentGateway} Call fake payment API outside DB transaction
//        ↓
// If payment succeeds:
//        ↓
// DB transaction starts
//        ↓
// G1:: Mark order [STATE: SUCCESS] + write outbox/audit
//        ↓
// DB transaction commits
//
// If payment fails after retry:
//        ↓
// DB transaction starts
//        ↓
// G2:: Mark order [STATE: FAILED] + write outbox row for compensation
//        ↓
// DB transaction commits
