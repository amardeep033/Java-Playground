# 01 RabbitMQ + Spring Boot

Two small services for studying reliable message delivery: one topic exchange, three queues,
manual acknowledgements, idempotency, Spring listener retry, and delayed broker retry.

## 1. How To Run And Read The Project

### 1.1 Prerequisites

| Requirement | Use |
| --- | --- |
| Java 17+ and Maven | Build and run both independent Spring Boot projects. |
| Docker and permission to run `sudo docker` | The publisher script starts the local RabbitMQ container. |
| Bash and `lsof` | Run scripts and publisher port checks. |
| Ports 8080, 5672, 15672 | Publisher HTTP API, RabbitMQ AMQP, RabbitMQ management UI. |

All commands below start from `01_rabbitmq/` unless noted otherwise.
The `learner / learner` credentials are for this local learning container.

### 1.2 Start RabbitMQ And Both Services

Terminal 1:

```bash
./publisher/run.sh
```

The script starts or reuses `rabbitmq-learning`, then starts the publisher on port 8080.
It stops an existing process listening on that port and waits for the port to be released.
Open [RabbitMQ management](http://localhost:15672) and log in with **learner / learner**.
Wait until RabbitMQ is ready before publishing; a newly created broker can take a few seconds.

Terminal 2:

```bash
./consumer/run.sh
```

Only `learning.main` has listeners: two consumer threads in this one application instance.
The retry queue and DLQ have no listeners. Stop older demo instances to avoid competing consumers.

### 1.3 Publish The Three Cases

```bash
curl -X POST 'http://localhost:8080/publish?message=hello'
curl -X POST 'http://localhost:8080/publish?message=hello2'
curl -X POST 'http://localhost:8080/publish?message=hello3'
```

| Payload | Expected Behavior | Where To Watch |
| --- | --- | --- |
| `hello`, `hello1`, or other ordinary text | Process successfully, record ID, manually ACK. | `PROCESS` and `ACK` logs; main queue drains. |
| `hello2` | Always simulate a temporary failure; Spring attempts, delayed rounds, then DLQ. | Repeated `PROCESS`, two `RETRY QUEUE` logs, then `learning.dlq`. |
| `hello3` | Simulate a permanent failure; reject immediately without requeue. | One processing attempt, `REJECT -> DLQ`, then `learning.dlq`. |

The API returns `Sent: <messageId>` after publisher confirmation and routing checks.
That response does not mean the consumer has finished processing.

### 1.4 Observe Idempotency

```bash
curl -X POST 'http://localhost:8080/publish?message=hello&messageId=event-1'
curl -X POST 'http://localhost:8080/publish?message=hello&messageId=event-1'
```

The second delivery logs `duplicate=true` and is ACKed without repeating the work.
Omitting `messageId` generates a new UUID. Reuse an ID only for another delivery/publication of
that same event. A fresh consumer process starts with an empty in-memory idempotency set.

### 1.5 Read The Code In This Order

| No. | File | Follow This Responsibility |
| --- | --- | --- |
| 1 | [PublisherController](publisher/src/main/java/learning/publisher/controller/PublisherController.java) | HTTP request enters `/publish`. |
| 2 | [PublisherService](publisher/src/main/java/learning/publisher/service/PublisherService.java) | Encode payload, set ID/persistence, publish, check confirm and return. |
| 3 | [Publisher RabbitMqConfig](publisher/src/main/java/learning/publisher/config/RabbitMqConfig.java) | Declare exchange, queues, bindings, TTL, and DLX routes. |
| 4 | [MessageListener](consumer/src/main/java/learning/consumer/listener/MessageListener.java) | Read delivery metadata; manually ACK success/duplicates or reject permanent failures. |
| 5 | [ConsumerService](consumer/src/main/java/learning/consumer/service/ConsumerService.java) | Idempotency and simulated business failures. |
| 6 | [ListenerConfig](consumer/src/main/java/learning/consumer/config/ListenerConfig.java) | Configure the listener factory, Spring retry, and manual recovery ACK behavior. |
| 7 | [RetryRecovery](consumer/src/main/java/learning/consumer/service/RetryRecovery.java) | After Spring attempts, choose confirmed forwarding to retry or rejection to DLQ. |
| 8 | [Consumer RabbitMqConfig](consumer/src/main/java/learning/consumer/config/RabbitMqConfig.java) | Declare the same broker topology so either app can create it first. |
| 9 | [Consumer properties](consumer/src/main/resources/application.properties) | Attempt count/backoff, manual mode, concurrency, and prefetch. |

Build each project independently:

```bash
mvn -f publisher/pom.xml clean package
mvn -f consumer/pom.xml clean package
```

### 1.6 Stop And Troubleshoot

Use **Ctrl+C**, not Ctrl+Z, in each service terminal. Stop the broker when finished:

```bash
sudo docker stop rabbitmq-learning
```

The container uses `--rm` and no persistent volume: stopping it removes its broker data.

| Symptom | Check / Action |
| --- | --- |
| Docker socket permission denied | The publisher script uses `sudo docker`; enter your local sudo password when prompted. |
| Port 8080 already occupied | Use the publisher run script; Ctrl+Z may have suspended an old publisher while retaining its port. |
| Connection refused on 5672 | Check RabbitMQ readiness and the management UI; restart the app if needed. |
| `PRECONDITION_FAILED / inequivalent arg` | A queue/exchange already exists with incompatible settings. Compare declarations first; drain data before any deliberate deletion. |
| Publisher logs success, but this consumer logs nothing | Inspect the queue's consumers: another instance may be consuming. Two registrations are expected for one demo consumer instance. |
| Queue shows zero messages | Normal after successful ACK; inspect logs and publish/delivery/ACK rates. |
| Old learning queues remain in the UI | They may belong to earlier lessons. This code does not delete old queues or messages. |
| Maven reports `spring-boot:run ... exit code: 1` | Read the earlier application exception; the Maven footer is only the process-exit summary. |

## 2. Notes

### 2.1 Architecture And Boundaries

| Component | Responsibility | Important Boundary |
| --- | --- | --- |
| Publisher | Publishes payload and properties to a named exchange with a routing key. | Does not wait for downstream business processing. |
| Exchange | Applies its type's routing algorithm to its bindings. | Does not scan every queue for a matching name. |
| Binding | Connects an exchange to a queue using a key/pattern or arguments. | A queue can have multiple bindings from multiple exchanges. |
| Queue | Stores routed messages pending delivery/settlement. | Queue name and routing key are separate concepts. |
| Consumer | Subscribes to named queues and processes deliveries. | Competing consumers share work; separate queues give services independent copies. |
| Virtual host | Namespaces queues, exchanges, bindings, and permissions. | The same queue name in different virtual hosts identifies different queues. |
| Management UI | Manages broker resources through the management plugin/API. | UI-created and application-declared queues must have compatible settings. |

### 2.2 Routing Classification

| Exchange / Pattern | Matching Rule | Example |
| --- | --- | --- |
| Direct | Routing key exactly matches a binding key. | `order.created` matches `order.created`. |
| Default | Built-in direct exchange named `""`; automatic queue-name binding. | Routing key equals the destination queue name. Not used by this demo. |
| Topic | Routing key matches binding patterns. | This demo uses `learning.events` with exact topic keys. |
| Fanout | Ignores routing key; routes to every bound queue. | Broadcast with one queue per independently interested service. |
| Headers | Matches configured message headers. | Routing selected by header values and binding arguments. |
| Topic `*` | Matches exactly one dot-separated word. | `order.*` matches `order.created`, not `order.eu.created`. |
| Topic `#` | Matches zero or more dot-separated words. | `order.#` matches `order` and `order.eu.created`. |
| Multiple matching bindings | One queue receives one routed copy per publication even if several bindings match. | `order` and `order.#` can both match the same queue. |

Dots in queue names are a naming convention; topic pattern semantics apply to routing/binding keys.
Changing bindings changes destinations, not the exchange's declared type.

### 2.3 This Project's Topology

| Resource | Binding / Arguments | Result |
| --- | --- | --- |
| Topic exchange `learning.events` | Durable, not auto-delete. | All application publishing and dead-letter routes use it. |
| Main `learning.main` | Binding `work.main`; DLX `learning.events`, dead-letter key `work.dlq`. | Consumer processing; rejected deliveries route to DLQ. |
| Retry `learning.retry` | Binding `work.retry`; message TTL 5000 ms; DLX key `work.main`. | No consumer; expired messages route back to main. |
| DLQ `learning.dlq` | Binding `work.dlq`; no TTL or return DLX. | No consumer; retain for manual inspection/replay. |

```text
publish -> learning.events / work.main -> main -> successful processing -> ACK
                                            | permanent failure / exhausted rounds
                                            +-- reject without requeue -> DLQ

Spring attempts exhausted -> publish copy / work.retry -> retry queue
                          -> confirm + ACK original          |
                                                    message TTL 5s
                                                            |
                                  main <- work.main / learning.events
```

A DLX is an exchange used for dead-letter routing, not a special queue type. The same topic
exchange can serve normal publishing and dead-letter routes using different keys.

### 2.4 Message Data And Identifiers

| Item | Who Sets It? | Scope / Purpose |
| --- | --- | --- |
| Body | Publisher | Bytes; text, JSON, or binary are possible. This demo encodes plain text as UTF-8. |
| `contentType` / `contentEncoding` | Publisher | Metadata describing payload format/encoding; metadata does not itself convert bytes. |
| `messageId` | Application | Stable event ID for tracking and application-managed deduplication; retained on retry copies. |
| Message `correlationId` | Application, if needed | Links related requests/replies or a workflow. Not set by this demo. |
| `CorrelationData` | Spring publishing app | Java object tracking a publish attempt's confirm/return; not the consumer's message correlation ID. |
| Delivery tag | RabbitMQ | Numeric, channel-scoped delivery identifier used for ACK/NACK/reject. |
| `redelivered` | RabbitMQ | Signals previous delivery; does not prove successful processing or count TTL/DLX rounds. |
| `retry-round` | `RetryRecovery` | Application-managed delayed-round counter, preserved with the message. |
| `x-death` | RabbitMQ | Dead-letter history; useful for diagnostics, not the counter used by this implementation. |

### 2.5 Publisher Confirms And Returns

| Configuration / Check | What It Establishes | What It Does Not Establish |
| --- | --- | --- |
| `publisher-confirm-type=correlated` | Correlate broker ACK/NACK to the publishing attempt. | Consumer completion or exactly-once business execution. |
| `publisher-returns=true` | Enable handling returned publications. | A guarantee that every publication routes. |
| `template.mandatory=true` | Request return when a publication cannot route to any queue. | A returned message does not necessarily receive a publisher NACK. |
| `future.get(5, SECONDS).isAck()` | Wait up to five seconds for confirmation, then check broker ACK. | A timeout does not prove the broker rejected or lost the publication. |
| `correlation.getReturned()` | Detect an unroutable returned publication. | An ACK alone is insufficient; both checks are needed. |
| Consumer-side publishing settings | Confirm copies sent to retry before settling the original. | Being a consumer does not prevent the same app from also acting as a publisher. |

| Publisher Confirm | Returned? | This Code's Outcome |
| --- | --- | --- |
| ACK | No | Publishing check succeeds. |
| ACK | Yes | Throw: unroutable publication. |
| NACK | Either | Throw: publication not accepted. |
| No confirm before timeout | Unknown | Timeout exception; outcome uncertain. |

### 2.6 Delivery States And Acknowledgements

| State / Action | Broker Behavior |
| --- | --- |
| Ready | Waiting in the queue for delivery to a consumer. Publishing does not immediately make it Unacked. |
| Unacked | Delivered to a consumer but not yet settled. |
| Consumer ACK | Remove the successfully handled delivery from its queue. |
| NACK/reject with `requeue=true` | Request redelivery to the same queue; may create a hot loop. Not used by this app's failure handling. |
| NACK/reject with `requeue=false` | Follow the queue's DLX configuration; discard when no DLX is configured. |
| Consumer channel/connection closes | Outstanding Unacked deliveries can be redelivered even though the app never requests `requeue=true`. |
| Consumer acknowledgement timeout | Broker can close the channel and return outstanding work; behavior depends on broker configuration/version. |
| No consumer | Keep messages subject to TTL, limits, expiry, and policies; no consumer does not itself imply message expiry. |

| Spring Acknowledgement Mode | Successful Return | Failure / Risk |
| --- | --- | --- |
| `manual` | Application must ACK; return alone does not settle a delivery. | This app explicitly rejects permanent failures and configures recovery settlement. |
| `auto` | Container handles ACK after successful listener completion. | Retry/error handling decides failed-delivery settlement; not simply “return = ACK, exception = basicReject.” |
| `none` | Broker treats delivery as acknowledged without waiting for processing. | Processing can fail after the broker has already released responsibility. |

### 2.7 Connection, Channel, And Batch Tags

| Concept | Meaning |
| --- | --- |
| Connection | TCP communication with RabbitMQ, usually long-lived. |
| Channel | Logical AMQP session within a connection; this demo's consumer threads use separate channels. |
| Delivery tag | Monotonic delivery sequence within a channel; two channels can both have tag 7. |
| `basicReject(tag, requeue)` | Reject one delivery. |
| `basicNack(tag, multiple, requeue)` | Negatively acknowledge one delivery or outstanding tags up to the supplied tag on the same channel. |
| `multiple=true` | Affects outstanding tags `<= tag`; it does not affect Ready messages, already ACKed deliveries, or another channel. |

Example: `basicNack(7, true, false)` on a channel that has these deliveries:

| Message / Delivery Tags | Current State | Affected? |
| --- | --- | --- |
| 1, 2, 3 | Already ACKed | No. |
| 4, 5, 6 | Unacked on this channel | Yes. |
| 7 | Current delivery, Unacked | Yes. |
| Messages labelled 8, 9, 10 | Still Ready | No; they do not yet have consumer delivery tags. |

This example needs several outstanding deliveries on one channel. With this demo's single queue
and `prefetch=1`, each consumer normally has only one outstanding delivery, so batching adds no benefit.

### 2.8 Routes And Their Owners

| No. | Path | Trigger | Who Moves / Settles It? |
| --- | --- | --- | --- |
| 0 | Publisher → main | Initial HTTP request | `PublisherService` publishes with `MAIN_KEY`; topic binding selects main. |
| 1 | Main → retry | Spring attempts fail and delayed rounds remain | `RetryRecovery` publishes a copy with `RETRY_KEY`, confirms routing/acceptance, then triggers ACK of original. |
| 2 | Main → DLQ | Permanent processing failure or missing message ID | Listener calls `basicReject(tag, false)`; broker follows main's configured DLX route. |
| 3 | Retry → main | Retry queue's message TTL expires | RabbitMQ follows retry's DLX/key; no listener or application code moves it. |
| 4 | Main → DLQ | Delayed rounds exhausted or retry publication fails | Recovery throws `AmqpRejectAndDontRequeueException` with manual rejection enabled; container rejects without requeue. |

There is no direct retry-queue → DLQ route: the message returns to main, fails processing again,
and recovery decides whether more delayed rounds remain.

### 2.9 Spring Attempts Versus Delayed Rounds

| Mechanism | Counter / Delay | Where The Message Waits |
| --- | --- | --- |
| Spring listener retry | Three attempts per delivery, including first; one second between attempts. | Original remains Unacked; listener thread participates in retry/backoff. |
| Delayed broker retry | `retry-round` increases on each confirmed forwarding; five-second TTL. | Ready in retry queue; main consumer thread is released after recovery ACK. |
| Round limit | `MAX_RETRY_ROUNDS = 2`. | Applied in recovery after the current batch of Spring attempts fails. |
| Permanent failure | Caught inside listener before exception reaches interceptor. | No Spring backoff or delayed round; reject to DLQ immediately. |

| Incoming `retry-round` | Spring Processing Attempts | After Failure |
| --- | --- | --- |
| Absent / 0 | Attempts 1–3 | Publish retry copy with round 1. |
| 1 | Attempts 4–6 | Publish retry copy with round 2. |
| 2 | Attempts 7–9 | Reject to DLQ. |

An always-failing `hello2` therefore makes **9 processing attempts** over roughly **16 seconds**:
three batches with two one-second waits each, plus two five-second queue waits. Load, restarts,
connection failures, or duplicate deliveries can change timing and total observed executions.

### 2.10 Spring Wiring And Manual Recovery

| Element | Role |
| --- | --- |
| `@RabbitListener` | Registers `MessageListener.consume()` as a queue listener. |
| Bean `rabbitListenerContainerFactory` | Creates/configures the listener container used by the annotation. |
| `configurer.configure(...)` | Applies Boot properties and the `MessageRecoverer` bean to the retry interceptor. |
| `RetryRecovery implements MessageRecoverer` + `@Component` | Makes custom recovery available after Spring attempts are exhausted. |
| Uncaught `IllegalStateException` | Temporary demo failure; reaches Spring retry. |
| Caught `IllegalArgumentException` | Permanent demo failure; listener rejects directly, so Spring retry sees no processing exception. |
| `ImmediateAcknowledgeAmqpException` | Signals successful retry forwarding; `enforceImmediateAckForManual=true` enables the container's ACK in manual mode. |
| `AmqpRejectAndDontRequeueException(..., true, cause)` | Requests rejection even in manual mode; main's DLX then selects DLQ. |

The demo assigns exception meaning through its own handling; Spring does not inherently consider
all `IllegalArgumentException`s permanent or all `IllegalStateException`s temporary.

### 2.11 Queue Properties And Declaration

| Setting / Method | Behavior | Distinction |
| --- | --- | --- |
| `durable(name)` | Retain queue definition across broker restart when storage is retained. | Does not make every message persistent or preserve a deleted container's data. |
| `exclusive()` | Restrict to declaring connection; delete when it closes. | Connection ownership, not a consumer thread limit. |
| `autoDelete()` | Delete when last consumer leaves after queue has had a consumer. | Not the same as “never had a consumer.” |
| `ttl(ms)` | Set a message TTL on the queue. | A wait/expiry rule, not an attempt limit or queue lifetime. |
| `expires(ms)` | Delete queue after configured disuse. | Queue expiration does not dead-letter all its contents. |
| `maxLength(n)` / `maxLengthBytes(n)` | Limit Ready message count / body bytes. | Overflow policy decides whether messages are dropped, dead-lettered, or publishing is rejected. |
| `maxPriority(n)` | Configure supported message priorities. | Support/semantics depend on queue type. |
| `deadLetterExchange(name)` | Select exchange for dead-lettered messages. | Bindings still select queues. |
| `deadLetterRoutingKey(key)` | Override dead-letter routing key. | Without override, original routing keys are used. |
| `withArgument(key, value)` | Set supported queue arguments, e.g. queue type. | Unsupported/inequivalent declarations can fail. |
| Repeated declaration | Reuse a resource when declaration settings match. | Publishers, consumers, UI, or infrastructure tooling can declare it; no dedicated manager service is required. |

Dead-letter triggers include reject/NACK without requeue, message TTL expiry, applicable queue-length
overflow, and configured quorum delivery-limit handling. Mutable broker policies can be preferable
to hardcoded queue arguments for production operational changes.

### 2.12 Durability And Queue Classification

| Exchange | Queue | Message | Restart Outcome With Retained Broker Storage |
| --- | --- | --- | --- |
| Durable | Durable | Persistent | Intended to recover subject to acceptance and broker durability guarantees. |
| Durable | Durable | Non-persistent | Queue survives; message recovery is not guaranteed. |
| Non-durable | Durable | Persistent | Already-routed message can recover from queue; exchange must be recreated. |
| Durable | Non-durable | Persistent | Queue disappears, so its message cannot be recovered from that queue. |

| Queue Type | Storage / Replication | Decision |
| --- | --- | --- |
| Classic | Non-replicated in current RabbitMQ; durability/persistence still matter. | Simple local work queue; this demo relies on the broker's default queue type. |
| Quorum | Replicated queue using consensus. | Consider when replicated availability is required; configure members and delivery limits deliberately. |

### 2.13 Concurrency, Ordering, And Backpressure

| Concern | Behavior / Habit |
| --- | --- |
| Two consumers in one service | `concurrency=2`, `max-concurrency=2` create two consumer threads. |
| Multiple instances | Each instance creates its own configured consumers; two instances here normally register four. |
| Prefetch | `prefetch=1` limits one Unacked delivery per consumer; two consumers allow two in this single-queue demo. |
| Rate limiting | Prefetch bounds in-flight work, not messages per second; use separate rate limiting if downstream needs it. |
| Ordering | Queue order does not guarantee business completion order with multiple consumers, redelivery, or priority. |
| Queue growth | Sustained input above completed-processing rate grows backlog; retries add load without completing business work. |
| Independent subscribers | Bind a separate queue for each service that needs its own copy. |
| Thread safety | Listener/service beans are shared; this demo synchronizes its short check/process/record section. |

### 2.14 Idempotency, Outbox, And Failure Windows

| Concern | This Demo | Production Direction |
| --- | --- | --- |
| Duplicate delivery | Successful IDs are kept in a `HashSet`; duplicates ACK without repeating work. | Unique event ID and business changes committed in one database transaction. |
| Failed processing | Does not add ID to the successful set. | Never mark failed work completed. |
| Multiple app instances | In-memory set is not shared. | Shared durable inbox/processed-event storage. |
| Restart / memory | Set resets on restart and is unbounded. | Retention/cleanup and durable state appropriate to replay needs. |
| Database update + initial publish | No database in this demo. | Transactional outbox plus CDC/poller bridges commit/publish gaps; relay duplicates still need idempotency. |
| Confirmed retry publish + original ACK | Two separate operations. | A crash between them can create duplicates; design for idempotent processing. |
| Classic DLX forwarding | Broker-managed handoff has failure limits. | Evaluate replicated queues and supported at-least-once dead-lettering when required. |
| DLQ replay | Explicit manual republish after diagnosing the cause. | Preserve event identity and deliberately manage retry headers; do not blindly replay unchanged poison messages. |

### 2.15 RabbitMQ Queues Versus Kafka Topics

| Concern | RabbitMQ Queue Model | Kafka Traditional Consumer Groups |
| --- | --- | --- |
| Successful processing | ACK removes message from queue. | Offset commit records progress; record remains subject to retention/compaction. |
| Replay | ACKed messages are not normally available from that queue. | Seek/reset offsets while records remain available. |
| Independent service copies | Separate queues bound to matching exchanges/keys. | Separate consumer groups. |
| Ordering | Concurrency/redelivery affect delivery and completion order. | Log order is per partition; processing must preserve it. |
| Failure handling | App retries, requeue, DLX/retry/DLQ topology. | App/framework retry handling, optionally retry/DLQ topics. |
| Typical fit | Task delivery and work distribution. | Retained event streams and replay. |

This comparison covers ordinary RabbitMQ queues, not RabbitMQ streams.

### 2.16 Monitoring And Operational Habits

| Signal / Concern | Interpretation / Action |
| --- | --- |
| Ready count / oldest message age | Detect growing backlog and latency. |
| Unacked count | Detect occupied consumers, slow work, or missing settlement. |
| Publish / delivery / ACK rate | Separate incoming load, consumer delivery, and completion/settlement. |
| Retry / DLQ growth | Distinguish temporary outages, poison data, and exhausted recovery budgets. |
| Connection loss / shutdown | Allow active work to finish where possible; expect redelivery of unsettled deliveries. |
| Confirms and returns | Handle NACK, unroutable messages, and uncertain timeout outcomes. |
| Permissions / credentials | Demo credentials are local; production uses managed secrets, least-privilege virtual-host access, and protected transport. |
| Broker storage | Durable declarations do not survive deleting the only storage; use retained volumes/backups as appropriate. |

### 2.17 Reference Sources

| Topic | Primary Reference |
| --- | --- |
| Delivery ACKs and publisher confirms | [RabbitMQ acknowledgements and confirms](https://www.rabbitmq.com/docs/confirms) |
| TTL and expiry | [RabbitMQ TTL](https://www.rabbitmq.com/docs/ttl) |
| DLX and failure routing | [RabbitMQ dead-letter exchanges](https://www.rabbitmq.com/docs/dlx) |
| Queue properties | [RabbitMQ queues](https://www.rabbitmq.com/docs/queues) |
| Consumer behavior and timeouts | [RabbitMQ consumers](https://www.rabbitmq.com/docs/consumers) |
| Retry/recovery | [Spring AMQP recovery](https://docs.spring.io/spring-amqp/reference/amqp/resilience-recovering-from-errors-and-broker-failures.html) |

## 3. Interview QA — Tricky And Scenario Questions

| No. | Scenario / Question | Answer |
| --- | --- | --- |
| 1 | Publisher receives ACK, yet no queue contains the message. Is that contradictory? | No. Unroutable publications can be confirmed. Mandatory returns detect no matching route; also check whether a consumer already ACKed the delivery. |
| 2 | Confirm wait times out. Is it safe to republish with a new ID? | Outcome is uncertain; the original may have reached the broker. Reuse the event's stable ID and make downstream processing idempotent. |
| 3 | Consumer updates the database, then crashes before ACK. What happens? | Delivery can repeat. Commit processed-event identity with business changes atomically so redelivery does not duplicate side effects. |
| 4 | Retry copy is confirmed, but consumer crashes before settling the original. | Original and copy can both be delivered. Confirms plus ACK are not one atomic transaction. |
| 5 | Why not simply NACK without requeue to send temporary failures to retry here? | Main's configured DLX key points to DLQ. This app explicitly republishes temporary failures to the retry key instead. |
| 6 | Why not use `requeue=true` with a sleep? | It returns to the same queue and can hot-loop; sleeping occupies a consumer thread. Retry TTL lets the broker hold work between rounds. |
| 7 | Can retry TTL alone cap retries at three? | No. TTL controls message age in a queue. An application/broker-supported counter and a stopping decision are required. |
| 8 | Where is the retry → DLQ decision made? | There is no direct route here: TTL returns to main; after another failed batch, recovery checks the delayed-round limit. |
| 9 | Three Spring attempts and two delayed rounds mean how many attempts? | Nine in the normal always-failing path: three attempts in each of the original, round-1, and round-2 deliveries. |
| 10 | Why does `hello3` not receive Spring retries? | Listener catches its permanent exception, rejects the delivery, and returns normally. The retry interceptor never sees that processing exception. |
| 11 | Does `return false` from the service ACK a duplicate? | No. The listener still explicitly ACKs; service return values have no broker settlement effect. |
| 12 | Can `redelivered=true` be used as proof that work completed? | No. Earlier delivery may have crashed before processing. Check durable successful-event identity instead. |
| 13 | Why are publisher-confirm settings also in the consumer app? | It publishes retry copies and must confirm their handoff before ACKing originals. |
| 14 | What if the retry publication is unroutable? | Recovery detects the return and requests rejection without requeue; main's DLX routes the original to DLQ. |
| 15 | In manual mode, why can `ImmediateAcknowledgeAmqpException` settle a delivery? | This project's factory explicitly enables immediate ACK enforcement for manual mode after successful recovery forwarding. |
| 16 | `basicNack(7, true, false)` while tags 1–3 are ACKed and 4–7 are Unacked: which are affected? | Only 4–7 on that channel. Already ACKed, another channel's deliveries, and Ready messages are unaffected. |
| 17 | Two channels both have tag 7. Can one channel ACK the other's delivery? | No. Tags are channel-scoped; use the channel that received the delivery. |
| 18 | Main is empty and this terminal shows no delivery. Was the message lost? | Check other consumers and rates. Another instance can receive and ACK it; a zero Ready count alone proves little. |
| 19 | Two services must each receive every event. Add consumers to one queue? | No. They would compete. Use separate queues with matching bindings for independent copies. |
| 20 | Queue was created in the UI. Can app declaration reuse it? | Yes, with compatible settings in the same virtual host. Incompatible durable/argument settings cause declaration failures. |
| 21 | Durable queue and persistent messages disappear after stopping this container. Why? | The disposable container has no retained volume. Durability does not preserve storage that was removed. |
| 22 | Can a non-durable exchange destroy an already-routed persistent message on restart? | The exchange disappears, but an accepted message may still recover in its durable destination queue. Future publications need a recreated exchange. |
| 23 | Two instances each have concurrency 2 and prefetch 1. Maximum normal in-flight deliveries here? | Four across the single-queue consumers; successful completion order can differ from enqueue order. |
| 24 | Can one topic publication matching two bindings deliver two copies to the same queue? | Matching multiple bindings to that queue still routes one copy for that publication; later redelivery is a different mechanism. |
| 25 | Does synchronizing the in-memory set solve production idempotency? | Only within this process and lifetime. It neither persists across restarts nor coordinates separate instances. |
| 26 | Does a DLQ automatically retry after the outage ends? | Not this one: no listener, TTL, or return DLX. Inspect and explicitly replay only after correcting the cause. |
| 27 | After moving to manual ACK, Unacked grows while listeners return successfully. Why? | Return alone does not ACK. Check every success/duplicate/failure path and the configured recovery settlement behavior. |
| 28 | Why can the retry queue have Ready messages despite zero consumers? | It intentionally holds them. TTL expiration triggers DLX routing; no consumer is needed to move them back. |
| 29 | What if the database commits but initial publishing never happens? | A transactional outbox records the publication intent in the same commit; a CDC/poller relay publishes later. |
| 30 | Why can processing exceed the nominal nine attempts during failures? | Broker redelivery, crashes, duplicate publications, and uncertain handoffs are outside the normal bounded round sequence. Idempotency remains necessary. |
