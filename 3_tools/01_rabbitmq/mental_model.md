1. why rabbitmq
- we dont need to wait for response, callback is an option but (a) when multiple service its an issue and (b) any change is an issue as tightly coupled

2. how to use 
- use outbox pattern and a cdc or poller to publish from it

3. what is rabbitmq
- broker which makes use of Advanced Message Queuing Protocol(AMQP: 5672) for communication 

4. how different from kafka
- in rabbit mq, once processed removed but in kafka(distributed log) retained for replay 

5. rabbit mq arch:
- Producer -> Exchange -> Binding -> Queue -> Consumer
- Each queue has "queue name" and "binding keys" wrt to "exchange name"
- Queue A: {(E1 -> k1, k1.#, k2, k3), (E2 -> k4.*)}
- each msg(string/object -> JSON bytes) has "exchange name" and "routing key"
- if msg and routing key matches -- places in the queue once -- can place in multiple queue
- consumer only about "queue name", and same fn can listen to n queues(no regex here)

6. creation and uses:
- think of it like db and tables terms
- the queues are durable as stored in disk --- durable queue and persistent msg
- there is no central place to create queues like how we create table in db ui
- any service can create a queue and if already exist with same configuration then it directly uses it
- although UI(http://localhost:15672) is provided and we create a queue -- its like creating from some service
- if we want centralized, have a dedicated service for queue config and management
- Also note that queue can be of two types:
    - classic: one node
    - quoram: replicated n node

7. There are five types of exchange: 
- direct exchange: routing key must exactly match a binding key
    - default exchange: special case of direct exchange where exchnage name is "" signifying routing key = queue name and no explicit binding required here
- topic exchange: routing key matches binding patterns
- FANOUT exchange: Sends a copy to every queue bound to the exchange, ignoring the routing key.
- HEADERS exchange: Routes based on message headers rather than the routing key.

8. multiple producer and consumer
- any producer can put in same queue
- any listener listening to it can listen -- only one 
- for multiple listener : it can be same service different thread OR different service
- spring.rabbitmq.listener.simple.concurrency=2 and spring.rabbitmq.listener.simple.max-concurrency=2(in same service, as a pool of listener threads)
- if you want two listener to listen to same msg -- then have separate queue for it

9. lifecycle
- any msg sent is of unack status
- now user has three option: ack it(ACK), negative ack it(NACK) or reject it(REJECT)
- once ack, the msg is removed 
- once reject, it is discarded and if dlq is set up then put in it
- one nack -- you have three option :
    - requeue : true --- retry instantly
    - requeue : false --- discard OR if dlq is set up then put in it OR maintain a queue for retry after sometime -- and on exhaust move to dlq if set up (still need a way to track attempts and stop retrying after a limit)
- so basically we can have three queue
    - main : listener always listens from it + if you want to retry instanseoutly
    - retry : if you want to retry after some time 
    - dlq : all discarded reject ones(poison msg too) --- can be manually replayed by republishing explicitly to main
- Note: since we have instant retry through main which people don't use as hot loop (if we dont want to make use of separate retry queue ) -- other option is spring listener retry here -- it maintains in ram of code for retry after delay before sending reject -- # spring.rabbitmq.listener.simple.retry.enabled=true

10. Note: we have three option to configure acknowledge: spring.rabbitmq.listener.simple.acknowledge-mode=manual
- manual: explicit ACK, NACK, REJECT
- none: delivery = ACK
- auto: return okay = ACK else REJECT(not NACK here)

11. redelivery
- it can happen to any listener(not necessarily first)
- order is followed but re-delivery can break that order
- make use of idempotency and simply ACK on idempotent case
- if something is UNACK -- there is no specified timeout

12. other options
- spring.rabbitmq.publisher-confirm-type=correlated -- for pub to know that rabbitmq has accepted resp(only ACK and NACK -- not REJECT) -- ack even if no queue matches
- spring.rabbitmq.publisher-returns=true -- Unroutable messages (no matching queue) are returned to the producer via a returns callback
- spring.rabbitmq.template.mandatory=true -- mandatory to publish the msg -- so if no queue matches then NACK
- spring.rabbitmq.listener.simple.prefetch=1 -- how many can wait without ack in a listener thread

13. Monitoring and troubleshooting
Metric	What it tells you
Ready messages	Messages waiting to be delivered to consumers
Unacked messages	Messages delivered but not yet acknowledged
Publish rate	How quickly producers send messages
Deliver/ack rate	How quickly messages are delivered and acknowledged
Oldest message age	How long messages have been waiting

14. Production-ready Spring Boot configuration
Manual vs. automatic acknowledgement: Choose acknowledgement behavior deliberately. With Spring AMQP AUTO, the container acknowledges after successful listener completion.
Retry and DLQ: Handle transient failures without creating endless requeue loops.
Prefetch and concurrency: Control in-flight messages and parallel processing according to downstream capacity.
Publisher confirms: Detect broker acceptance failures and uncertain outcomes.
Connection recovery and shutdown: Recover from connection interruptions and allow consumers to shut down gracefully.(A production-ready service should allow active processing to finish within a configured shutdown window)
Idempotency: Safely handle duplicate deliveries.
Observability: Monitor ready/unacked messages, consumer health, failure rates, and message age.