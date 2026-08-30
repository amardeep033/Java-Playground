package learning.publisher.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    
    // MAIN_QUEUE: {(EXCHANGE -> MAIN_KEY)}
    // RETRY_QUEUE: {(EXCHANGE -> RETRY_KEY)}
    // DLQ_QUEUE: {(EXCHANGE -> DLQ_KEY)}

    public static final String EXCHANGE = "learning.events";

    public static final String MAIN_QUEUE = "learning.main";
    public static final String RETRY_QUEUE = "learning.retry";
    public static final String DLQ_QUEUE = "learning.dlq";

    public static final String MAIN_KEY = "work.main";
    public static final String RETRY_KEY = "work.retry";
    public static final String DLQ_KEY = "work.dlq";

    @Bean
    public Declarables rabbitTopology() {
        // Exchanges can also have properties such as durable and autoDelete.
        // One queue can be bound to multiple exchanges.
        // Use a TopicExchange for pattern-based routing and a FanoutExchange for broadcasting to all bound queues.
        // Changing bindings cannot change the exchange type; the exchange type defines the routing algorithm.
        TopicExchange exchange = new TopicExchange(EXCHANGE);

        // Queues have properties such as message TTL and dead-letter routing settings, described below.
        // Queue declarations define broker properties and dead-letter routes through an exchange and its bindings.
        // They do not classify transient failures or set our application retry count; having no consumer means no listener subscribes.

        // 1. Retry:
        // The retry queue's TTL configuration does not enforce a retry limit.
        // You can implement a retry limit using:
        // A) application-managed headers,
        // B) RabbitMQ's dead-letter metadata, or
        // C) a retry mechanism that tracks attempts.
        // When the limit is reached, your application can route the message to the DLQ instead of the retry queue.

        // 2. Dead-lettering conditions (a configured DLX and matching binding determine the destination queue):
        // A consumer rejects a message with requeue=false.
        // A consumer negatively acknowledges a message with requeue=false.
        // The message expires, if applicable.
        // The queue exceeds a configured length limit and its overflow behavior dead-letters messages.


        // Main queue = durable + no message TTL + dead-letter route to the DLQ.
        // NOTE: After Spring retries a transient failure, we explicitly publish to the retry queue, confirm it, then ACK the original.
        // A NACK or REJECT with requeue=false instead follows this main queue's dead-letter route to the DLQ.
        // Reject and NACK are negative acknowledgements; both can request requeueing.
        // Reject affects one delivery; NACK can affect one delivery or multiple outstanding deliveries.

        // Delivery tag: RabbitMQ assigns a number per channel when delivering to a consumer; each consumer here uses its own channel.
        // MessageListener reads it using long tagC = message.getMessageProperties().getDeliveryTag().
        // With NACK(tagC, multiple=true, ...), all Unacked deliveries on that channel with tags <= tagC are affected.
        // requeue=true requests redelivery; requeue=false requests dead-lettering or discard.
        Queue main = QueueBuilder.durable(MAIN_QUEUE)
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(DLQ_KEY)
                .build();
        
        // Retry queue = durable + message TTL (delayed retry) + dead-letter route back to the main queue.
        // NOTE: No consumer listens here. Messages expire after their TTL and are dead-lettered back to the main queue.
        Queue retry = QueueBuilder.durable(RETRY_QUEUE)
                        .ttl(5000)
                        .deadLetterExchange(EXCHANGE)
                        .deadLetterRoutingKey(MAIN_KEY)
                        .build();

        // DLQ = durable.
        // NOTE: No consumer listens to this queue in our application.
        // Additional nuance: reject/NACK poison-message handling can differ for quorum queues by RabbitMQ version.
        // There is no expiry caused by the absence of a listener. No TTL or queue-expiry setting is configured here.
        Queue dlq = QueueBuilder.durable(DLQ_QUEUE)
                                .build();

        // Return one exchange, three queues, and three bindings for Spring RabbitAdmin to declare.
        return new Declarables(exchange, 
                                main, retry, dlq,
                                BindingBuilder.bind(main).to(exchange).with(MAIN_KEY),
                                BindingBuilder.bind(retry).to(exchange).with(RETRY_KEY),
                                BindingBuilder.bind(dlq).to(exchange).with(DLQ_KEY));
    }
}

// Method|What it does|Example
// .durable(name)|Queue definition survives broker restart when storage is retained; message persistence is separate|.durable("learning.main")
// .exclusive()|Queue is restricted to its declaring connection; deleted when that connection closes|.exclusive()
// .autoDelete()|Queue is deleted when it has had a consumer and its last consumer goes away|.autoDelete()
// .ttl(ms)|Sets a message TTL for this queue, not the queue's lifetime|.ttl(5000)
// .expires(ms)|Deletes the queue after it remains unused for the configured period|.expires(60000)
// .maxLength(n)|Limits the number of ready messages in the queue|.maxLength(10000)
// .maxLengthBytes(n)|Limits total body bytes of ready messages, excluding headers/properties|.maxLengthBytes(10485760)
// .maxPriority(n)|Configures supported message priorities; behavior depends on queue type|.maxPriority(10)
// .deadLetterExchange(name)|Sets the exchange to which dead-lettered messages are routed|.deadLetterExchange(EXCHANGE)
// .deadLetterRoutingKey(key)|Overrides the routing key used for dead-lettering|.deadLetterRoutingKey(MAIN_KEY)
// .withArgument(key, value)|Adds a queue argument; use a supported broker option|.withArgument("x-queue-type", "classic")

// NACK with multiple=true:
// When is this useful?
// - Batch consumers processing multiple messages together.
// - A batch operation fails and all outstanding deliveries up to the selected tag should be rejected.
// - Reducing acknowledgement-related network traffic in high-throughput consumers.

// Situation	What RabbitMQ does
// Queue has no consumer	Keeps messages queued, subject to queue limits, TTL and other policies
// Consumer receives a message but does not ACK it	Keeps it Unacked; a configured acknowledgement timeout can close the channel
// Consumer connection/channel closes	Requeues outstanding unacknowledged deliveries for redelivery
// Consumer NACKs with requeue=false	Dead-letters the message if configured, otherwise discards it
// Message expires in your retry queue	Dead-letters it to the configured exchange and routing key
