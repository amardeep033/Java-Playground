## 1. Curl

### 1.1 Start The App

```bash
./run.sh
```

### 1.2 Place A Checkout

```bash
curl -X POST http://localhost:8080/api/checkout \
  -H 'Content-Type: application/json' \
  -d '{
    "idempotencyKey": "demo-checkout-001",
    "customerEmail": "amar@example.com",
    "sku": "SKU-KEYBOARD",
    "quantity": 1
  }'
```

### 1.3 Verify Idempotency

Retry the same curl with the same `idempotencyKey`. It should return the existing order instead of creating a duplicate.

## 2. Cheatsheet

### 2.1 Sync, Async, Events, Saga

| No. | Classification | Topic | Meaning | In This Module |
| --- | --- | --- | --- | --- |
| 1 | Request/response | Sync REST API | Caller waits on the same HTTP request/response. | `POST /api/checkout` returns checkout result. |
| 2 | Async integration | Async event-driven | Producer emits event; other services react later. | Future outbox publisher sends order events to analytics/email/warehouse. |
| 3 | Distributed transaction | 2PC | Distributed transaction across systems. | Avoid for this study; usually heavy and operationally painful. |
| 4 | Distributed workflow | Saga orchestration | One orchestrator decides next step and compensation. | Easier to reason about, but orchestrator owns the workflow. |
| 5 | Distributed workflow | Saga choreography | Services react to each other's events. | Loosely coupled, but flow is harder to trace. |
| 6 | State/event model | State produces events | State change creates event. | `PENDING`, `SUCCESS`, `FAILED` produce forward or compensating actions. |
| 7 | State/event model | Forward action | Continue workflow after success. | `ORDER_SUCCESS` can trigger invoice/email/analytics. |
| 8 | State/event model | Compensating action | Repair already-committed work. | `ORDER_FAILED` can trigger stock release/refund. |

### 2.2 ACID

| No. | Letter | Meaning | Checkout Example |
| --- | --- | --- | --- |
| 1 | A - Atomicity | All or nothing transaction. | Reserve stock + create order + outbox row commit/rollback together. |
| 2 | C - Consistency | Preserve DB/business integrity. | Unique idempotency key prevents duplicate logical order. |
| 3 | I - Isolation | What one transaction can see while another changes data. | Concurrent checkouts should not oversell stock. |
| 4 | D - Durability | Committed data survives crash. | Committed order/outbox/audit rows remain after restart. |

### 2.3 DB Issues

| No. | Issue | What Happens | Typical Cause / Context |
| --- | --- | --- | --- |
| 1 | Dirty read | Transaction A reads data written by Transaction B before B commits; if B rolls back, A used invalid data. | Very low isolation like `READ UNCOMMITTED`. |
| 2 | Non-repeatable read | Transaction A reads the same row twice and gets different committed values. | Isolation below `REPEATABLE READ`. |
| 3 | Phantom read | Transaction A re-runs a range query and sees newly inserted/deleted matching rows. | Isolation below `SERIALIZABLE`, depending on DB. |
| 4 | Lost update | Two transactions read the same row, both modify it, and one write overwrites the other. | No locking/version check/atomic update in read-modify-write flow. |
| 5 | Write skew | Two transactions read overlapping data, each makes a valid change alone, but together violate a rule. | Snapshot-style isolation without a DB constraint or serializable check. |
| 6 | Dirty write | Transaction A overwrites uncommitted data written by Transaction B. | Usually prevented by real databases, but important conceptually. |
| 7 | Deadlock | Two or more transactions each hold a lock the other needs. | Circular wait on row/table/index/FK locks. |
| 8 | Starvation | One transaction repeatedly loses access to locks/resources. | High contention and unfair scheduling/priority. |
| 9 | Dual-write problem | DB write succeeds but message/cache write fails, or the reverse. | Writing DB + broker/cache without one durable coordination pattern. |
| 10 | Stale read | Read returns old data from a lagging replica/cache. | Async replication or cache lag. |
| 11 | Write-write conflict | Two transactions update the same row; one waits, fails, or retries. | Optimistic version failure or pessimistic lock contention. |
| 12 | Read skew | Related data is read at different times and forms an inconsistent view. | Non-repeatable read across multiple rows/tables. |
| 13 | Cache inconsistency | Cache and DB disagree. | Cache-aside without correct invalidation/update strategy. |
| 14 | Split brain | Two nodes both think they are primary and accept writes. | Network partition in distributed systems. |

Quick mapping to isolation levels:

| No. | Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read |
| --- | --- | --- | --- | --- |
| 1 | `READ UNCOMMITTED` | Possible | Possible | Possible |
| 2 | `READ COMMITTED` | Prevented | Possible | Possible |
| 3 | `REPEATABLE READ` | Prevented | Prevented | Possible in ANSI; some DBs prevent more. |
| 4 | `SERIALIZABLE` | Prevented | Prevented | Prevented |

### 2.4 Transaction Strategies

| No. | Strategy | Meaning | Code/Use |
| --- | --- | --- | --- |
| 1 | `readOnly = true` | Hint for query-only transactions. Do not treat it as security. | `findExistingResult()` |
| 2 | Transaction timeout | Fail slow transaction instead of waiting forever. | `@Transactional(timeout = 5)` |
| 3 | Rollback rules | Roll back for checked exceptions when needed. | `@Transactional(rollbackFor = PaymentException.class)` |
| 4 | `save()` -> `flush()` -> `commit()` | `flush()` sends SQL before commit. | Use when you want DB constraint failure now, not at method end. |
| 5 | Small transactions | Keep DB locks short. | DB transaction -> payment outside transaction -> DB transaction. |
| 6 | Atomic update | Check + update in one SQL statement. | `reserveStock()` |

### 2.5 Concurrency Strategies

| No. | Strategy | When Useful | In This Module |
| --- | --- | --- | --- |
| 1 | Atomic update | Simple stock decrement/check can be expressed as one SQL statement. | Implemented with `reserveStock()`. |
| 2 | Optimistic locking | Conflicts are uncommon; use version column and retry on conflict. | Interview option, not implemented. |
| 3 | Pessimistic locking | Conflicts are expected; lock row while making a longer decision. | Interview option, not implemented. |
| 4 | Higher isolation | Visibility rules matter across multiple reads/writes. | Discussed as tradeoff because it can reduce throughput. |
| 5 | Deadlock retry | Even correct locking can deadlock. | Keep transactions short, order locks consistently, retry safely. |

### 2.6 Isolation Levels

| No. | Level | Practical Meaning | Example Problem |
| --- | --- | --- | --- |
| 1 | `READ UNCOMMITTED` | Can potentially read data another transaction has not committed. | Dirty read. |
| 2 | `READ COMMITTED` | Common default; each statement sees committed data at that time. | Non-repeatable read. |
| 3 | `REPEATABLE READ` | Rows already read remain stable inside transaction. | Stronger read stability. |
| 4 | `SERIALIZABLE` | Strongest; conflicting transactions behave like one-at-a-time. | DB may abort one transaction; app must retry. |

### 2.7 Deadlock

| No. | Rule | Why |
| --- | --- | --- |
| 1 | Use consistent lock ordering. | Reduces circular wait. |
| 2 | Deadlocks can still happen. | Indexes, FK checks, and hidden DB locks can participate. |
| 3 | Detect -> rollback -> retry. | Deadlock victim transaction must be retried safely. |
| 4 | Keep transactions short. | Less time holding locks. |
| 5 | Avoid external calls inside transactions. | Payment/network wait should not hold DB locks. |
| 6 | Prefer atomic stock update here. | `UPDATE product SET stock = stock - 1 WHERE id = :id AND stock > 0` keeps the critical section small. |

## 3. Interview QA

### 3.1 Idempotency And Duplicates

| No. | Scenario | SDE2 Answer |
| --- | --- | --- |
| 1 | User clicks checkout twice or retries after timeout. | Use idempotency key. First request creates order; retry returns same order. Unique DB constraint is final guard. |
| 2 | Is idempotency same as locking? | No. Idempotency handles duplicate retries. Locking/isolation handles concurrent conflicting writes. |

### 3.2 Concurrency And Locking

| No. | Scenario | SDE2 Answer |
| --- | --- | --- |
| 1 | Two users buy the last item at same time. | Atomic update protects this code: `update stock = stock - qty where stock >= qty`. One succeeds; the other gets affected rows `0`. Other valid designs are optimistic or pessimistic locking. |
| 2 | What if atomic update is not enough? | Use optimistic locking for conflict detection, or pessimistic locking when you must hold a row lock across a longer decision. |
| 3 | How do you reduce deadlocks? | Keep transactions short, acquire locks in consistent order, avoid external calls inside transactions, and retry deadlock errors. |

### 3.3 Payment Failure And Recovery

| No. | Scenario | SDE2 Answer |
| --- | --- | --- |
| 1 | Payment times out. | Retry only retryable failures with backoff and payment-provider idempotency key. Do not hold DB transaction open while waiting. |
| 2 | Payment succeeds but server crashes before final confirmation. | Keep durable `PENDING` order state. On restart/retry, use idempotency/order state to safely confirm, retry, or compensate. |
| 3 | Payment succeeds but order confirmation/final update fails. | Retry using durable order/payment identifiers and saved state. If the business state cannot be repaired, compensate with refund/release stock. |

### 3.4 Transaction Boundaries

| No. | Scenario | SDE2 Answer |
| --- | --- | --- |
| 1 | Payment API is slow. Should DB transaction stay open? | No. Commit local PENDING state first, call payment outside DB transaction, then update final state. |
| 2 | Why use `REQUIRES_NEW` for audit? | Audit can commit even if main checkout transaction rolls back. Useful for traceability, but it creates independent truth. |

### 3.5 Events And Integration

| No. | Scenario | SDE2 Answer |
| --- | --- | --- |
| 1 | DB commits but Kafka publish fails. | Use outbox. Store event row in same DB transaction, then publish later with retry. |
| 2 | Why not only `@TransactionalEventListener` for cross-service events? | Timing is correct, durability is not. Use it for local after-commit work; use outbox for durable integration. |
| 3 | Is webhook/callback event-driven? | It is async HTTP/callback. True event-driven usually means durable broker/outbox semantics. |

### 3.6 Review Checklist

| No. | Scenario | SDE2 Answer |
| --- | --- | --- |
| 1 | What should be checked in AI-generated checkout code? | Transaction boundaries, external call placement, idempotency, rollback rules, outbox/dual-write risk, and compensation path. |

## 4. Current Runcontrol Experience

### 4.1 Flow

```text
Orchestrator calls Executor: POST /execute
        ↓
Executor returns 200 immediately: request accepted
        ↓
Executor does work later
        ↓
Executor calls Orchestrator: POST /update
        ↓
Orchestrator updates final execution status
```

This is not pure sync REST and not true event-driven architecture. It is async REST callback/webhook pattern.

### 4.2 What It Actually Is

| No. | Question | Answer |
| --- | --- | --- |
| 1 | Is it sync REST? | Not fully. `/execute` returns before actual work completes, so the caller is not waiting for the final result on the same HTTP response. |
| 2 | Is it event-driven? | Not in the broker-based architecture sense. There is no Kafka/RabbitMQ/SNS durability, buffering, redelivery, or pub/sub decoupling. |
| 3 | What is it? | Async request-reply over HTTP, also called callback/webhook or async-over-sync. |

### 4.3 Compared To True Event-Driven

| No. | Aspect | Your Setup | True Event-Driven |
| --- | --- | --- | --- |
| 1 | Transport | HTTP/REST: `/execute` then later `/update`. | Broker/pub-sub: Kafka, RabbitMQ, SNS, etc. |
| 2 | Coupling | Executor must know orchestrator `/update` URL. | Producer does not need to know all consumers. |
| 3 | Durability if receiver is down | Lost unless you add retry/persistence. | Broker persists until consumed, depending on config. |
| 4 | Retry/redelivery | You build it. | Broker/client handles redelivery patterns. |
| 5 | Buffering/backpressure | Not built in. | Broker can buffer spikes. |
| 6 | Ordering | Not built in. | Possible per queue/partition/key. |
| 7 | Failure recovery | Add retry, DLQ table, polling fallback, or status endpoint. | Broker + consumer retry/DLQ patterns. |

### 4.4 Practical Risks

| No. | Practical Risk | Mitigation |
| --- | --- | --- |
| 1 | Executor finishes work but crashes before calling `/update`. | Orchestrator polls `/status`, or executor stores callback retry state. |
| 2 | Orchestrator `/update` is down. | Executor retries with backoff and idempotency key. |
| 3 | `/update` is called twice. | Orchestrator update endpoint must be idempotent. |
| 4 | You need stronger durability/decoupling. | Introduce queue/broker or outbox-backed publisher. |

## 5. Webhook

Where it fits in this module: step `F`, the payment call. Almost nothing else changes, which is the point.

### 5.1 Why It Is Needed

`FakePaymentGateway.charge()` returns `boolean`, so it has two outcomes. A real payment call has three.

| No. | Outcome | Meaning | Can the code decide? |
| --- | --- | --- | --- |
| 1 | SUCCESS | Provider confirmed the capture. | Yes. Mark SUCCESS. |
| 2 | FAILED | Permanent decline, e.g. insufficient funds. | Yes. Mark FAILED and compensate. |
| 3 | UNKNOWN | Timeout, 5xx, connection dropped mid-capture. | No. Money may or may not have moved. |

`boolean` cannot express UNKNOWN. The webhook exists to resolve exactly that third case.

### 5.2 Flow

```text
F:: Initiate payment; provider returns PENDING/UNKNOWN
        ↓
Order stays [STATE: PENDING]; HTTP response returns "processing"
        ↓
... later: different thread, maybe different instance, maybe after a restart ...
        ↓
Provider calls back: POST /api/webhooks/payment
        ↓
Verify signature
        ↓
Deduplicate on provider event id
        ↓
G1/G2:: Drive the existing markSuccess() / markFailedAndQueueCompensation()
```

The webhook is a second entry point into the same state machine. It reuses `markSuccess()` and `markFailedAndQueueCompensation()` unchanged. That is why those sit in `CheckoutTransactionService` as separate transactional methods instead of being inlined into checkout.

It is also the payoff for durable state. The callback arrives on another thread, possibly another JVM, possibly after a restart, so it can only resume from a DB row.

### 5.3 Same Defenses, Inbound

| No. | Concern | Outbound equivalent in this module | Inbound version |
| --- | --- | --- | --- |
| 1 | Provider redelivers the same event many times. | Idempotency key (A). | Unique constraint on the provider event id. |
| 2 | `payment.failed` arrives after `payment.captured`. | - | One-way state machine rejects the regression. |
| 3 | Durable receipt before processing. | Outbox (C). | Inbox table. Same pattern, mirrored. |
| 4 | Must reply 200 within seconds or the provider retries. | - | Persist and return; process async. |

### 5.4 Two Problems With No Outbound Counterpart

| No. | Problem | Why it matters | Handling |
| --- | --- | --- | --- |
| 1 | The endpoint is public and unauthenticated. | Anyone who finds the URL can POST `payment.captured` and get free goods. | HMAC-verify the raw body before parsing it. This is a security requirement, not a design preference. |
| 2 | The callback can beat your own commit. | The provider may call back before `createPendingOrderAndReserveStock()` commits, so the lookup finds no order. | Do not return 404. Park the event in the inbox and retry, otherwise the payment is lost. |

### 5.5 The Part People Skip

Webhook delivery is best-effort, not guaranteed. Providers drop them.

| No. | Mechanism | Role |
| --- | --- | --- |
| 1 | Webhook | Makes the common case fast. |
| 2 | Reconciliation job | Makes it correct. Sweeps old PENDING orders and actively queries the provider status API. |

This is the same sweeper already needed for "payment succeeds but server crashes before confirmation".

### 5.6 Relation to Section 4

Runcontrol `/execute` then `/update` is this same pattern with you as the provider. `/update` is the webhook, so it needs the same rules: an idempotent handler, auth on the endpoint, out-of-order protection, and a polling fallback for dropped callbacks.
