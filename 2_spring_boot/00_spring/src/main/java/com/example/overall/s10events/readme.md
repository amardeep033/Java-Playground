# s10events

## How To Run

From `00_spring/`:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s10events.Main
```

## Expected Output Shape

```text
M1. Main -> Service
S1. Service creates order ORD-1
L1. @EventListener audit ORD-1
L2. @EventListener notify ORD-1
S2. Service continues after listeners finish
```

## Cheatsheet

| Topic | Default behavior | How to change / remember |
| --- | --- | --- |
| Event object | Plain Java object/record is enough. | Name events in past tense: `OrderCreatedEvent`. |
| Publisher | `ApplicationEventPublisher.publishEvent(event)` announces the event. | Publisher should not directly know every side effect service. |
| Normal listener | `@EventListener` runs synchronously by default. | `publishEvent(...)` returns after listeners finish. |
| Multiple listeners | All matching listeners run. | Use `@Order` only when ordering matters. |
| Listener ordering | Lower `@Order` value runs first. | `@Order(1)` before `@Order(2)`. |
| Async listener | Not async by default. | Use `@Async` + `@EnableAsync`. |
| Transaction-aware listener | Not used in this small code example. | Use `@TransactionalEventListener` when timing should follow commit/rollback. |

## Theory Not In Comments

| Theory | Meaning | When useful |
| --- | --- | --- |
| `@EventListener` | Runs immediately during `publishEvent(...)`. | Simple in-process reactions like audit log, cache warmup, metrics. |
| `@TransactionalEventListener` | Runs at a transaction phase; default is `AFTER_COMMIT`. | Send email only after DB commit succeeds. |
| `BEFORE_COMMIT` | Runs before commit completes. | Last checks/work before commit. |
| `AFTER_COMMIT` | Runs after successful commit. | Notifications, emails, external side effects. |
| `AFTER_ROLLBACK` | Runs only after rollback. | Failure audit/cleanup. |
| `AFTER_COMPLETION` | Runs after commit or rollback. | Cleanup that should happen either way. |
| No active transaction | Transactional event listener does not run by default. | Use `fallbackExecution = true` only if that fallback is intentional. |
| `@Async @EventListener` | Listener runs on an async executor. | Slow side effects that should not block publisher. |
| `@Async @TransactionalEventListener` | Transaction phase happens first, then async listener runs. | After-commit background email/notification. |

| Design choice | Prefer |
| --- | --- |
| Required business step | Direct service call, not event. |
| Optional side effect/reaction | Event listener. |
| Must happen only after DB commit | `@TransactionalEventListener(AFTER_COMMIT)`. |
| Slow side effect | Async listener. |
| Listener depends strongly on another listener | Avoid hidden ordering; make dependency explicit if it is business-critical. |

## Interview Q&A

| Question | Strong answer |
| --- | --- |
| What is a Spring event? | An object published inside the application so other beans can react without the publisher directly calling them. |
| What does `ApplicationEventPublisher` do? | It publishes an event into Spring's event infrastructure. |
| What does `@EventListener` do? | It marks a method as a listener for matching event types. By default it runs synchronously during `publishEvent`. |
| Are normal Spring events asynchronous? | No. Normal `@EventListener` is synchronous by default. Use `@Async` and enable async support when you want async handling. |
| If two listeners handle the same event, is order guaranteed? | Do not rely on it unless you explicitly set order, for example with `@Order`. |
| Why use events instead of direct service calls? | Events decouple side effects from the main use case, such as audit logs, emails, metrics, or notifications. |
| When should I avoid events? | If one operation is required for the main business result, keep the dependency explicit in the service flow. Events are best for reactions/side effects. |
| Why use `@TransactionalEventListener`? | It lets a listener run at a transaction phase, commonly after successful commit, so side effects do not happen for rolled-back work. |
| What is the default phase of `@TransactionalEventListener`? | `AFTER_COMMIT`. |
| What happens if the transaction rolls back? | `AFTER_COMMIT` listeners do not run. `AFTER_ROLLBACK` listeners do run. |
| What happens if there is no active transaction? | Transactional event listeners do not run by default. Use `fallbackExecution = true` if you intentionally want fallback execution. |
| Can `@Async` and `@TransactionalEventListener` be combined? | Yes. The listener is triggered at the transaction phase, then executed asynchronously. It should not assume it still has the original transaction context. |
| Why should event names often be past tense? | It keeps the model honest: an event announces something that already happened, such as `OrderCreatedEvent`, not a command like `CreateOrderEvent`. |
| Does publishing an event guarantee delivery like Kafka? | No. These are in-process Spring application events by default, not a durable message broker. |
