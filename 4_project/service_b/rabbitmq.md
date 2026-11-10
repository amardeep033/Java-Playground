# RabbitMQ flow

| Queue | Exchange / routing key | Consumer |
| --- | --- | --- |
| event-relay.delivery.main | event-relay.delivery / delivery.requested | B: webhook delivery |
| event-relay.delivery.retry | event-relay.retry / delivery.retry | none; TTL → main |
| event-relay.delivery.dlq | event-relay.dead / delivery.dead | inspection; replay later |
| event-relay.status.main | event-relay.status / delivery.completed | A: delivery projection |
| event-relay.status.dlq | event-relay.dead / status.dead | invalid status inspection |

- Durable queues/exchanges; persistent JSON messages; publisher confirms + mandatory returns.
- Both services declare the same topology. Default retry TTL: 30000ms.
- Outbox: B has one scheduler thread; A has two (outbox + reconciliation), DB claim leases + SKIP LOCKED, publish outside transaction, mark published after broker confirmation. Broker outages keep messages pending.
- B: four listener/worker threads, prefetch 1, manual ACK after DB state + outgoing messages commit. Unexpected DB failures requeue with 1s backoff.
- A: four completion listeners; inbox dedup + sequence checks; a terminal outcome is immutable. REST reconciliation applies the same contract and rules.
- Duplicate publication/redelivery is expected; message IDs remain stable on outbox retries. Delivery retries use a fresh message ID and the same delivery ID.
- Command contract v1; completion contract v2 includes every bounded attempt. Only SUCCEEDED/DEAD leave B; internal retries never create status events. Keep contracts aligned across services.
- B never reads A’s tables. It uses the snapshotted configuration and its own delivery state.
