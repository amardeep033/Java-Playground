package learning.consumer.service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import learning.consumer.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.ImmediateAcknowledgeAmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.stereotype.Component;


// Boot's container factory configurer uses our MessageRecoverer bean for custom recovery after listener retries.
@Component
public class RetryRecovery implements MessageRecoverer {
    private static final Logger log = LoggerFactory.getLogger(RetryRecovery.class);
    private static final int MAX_RETRY_ROUNDS = 2;
    private final RabbitTemplate rabbit;

    public RetryRecovery(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void recover(Message original, Throwable cause) {

        // Main queue  -- DLX --> DLQ
        // Retry queue -- DLX --> main queue after TTL
        // DLQ         -- no outgoing DLX

        // These are the message paths:
        // 0. First publication to main: PublisherService publishes through the exchange.
        // 1. Main to retry after temporary failure: explicitly publish a copy below, confirm it, then ACK the original if rounds remain.
        // 2. Main to DLQ on permanent failure: MessageListener rejects; RabbitMQ follows the configured DLX route.
        // 3. Retry to main: RabbitMQ routes expired messages through the retry queue's configured DLX after TTL.
        // 4. Main to DLQ on exhausted rounds or retry-forwarding failure below: reject the original through its configured DLX.

        // Entry is a failed delivery from main: 1. reject to DLQ if rounds are exhausted; 2. otherwise publish a copy to retry.

        // 1. Reject to DLQ when delayed retry rounds are exhausted.
        Object header = original.getMessageProperties().getHeaders().get("retry-round");
        int round = header instanceof Number value ? value.intValue() : 0;
        if (round >= MAX_RETRY_ROUNDS) {
            throw new AmqpRejectAndDontRequeueException("Retries exhausted -> DLQ", true, cause);
        }

        // 2. Publish a copy to retry when delayed rounds remain.
        // Each forwarding is a new publication; we increment the application-managed retry-round header.
        // Preserve the original messageId for idempotency; use a new CorrelationData ID for this publish attempt.
        Message cloned_msg_for_retry = MessageBuilder.fromClonedMessage(original)
                .setExpiration(null).setHeader("retry-round", round + 1).build();
        CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());

        // Use the same confirmed-publishing logic as the publisher, targeting the retry queue instead of main.
        try {
            rabbit.send(RabbitMqConfig.EXCHANGE, RabbitMqConfig.RETRY_KEY, cloned_msg_for_retry, correlation);
            if (!correlation.getFuture().get(5, TimeUnit.SECONDS).isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException("Retry publish was rejected or unroutable");
            }
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AmqpRejectAndDontRequeueException("Retry forwarding failed -> DLQ", true, failure);
        }

        log.info("RETRY QUEUE: id={}, round={}/{}", original.getMessageProperties().getMessageId(),
                round + 1, MAX_RETRY_ROUNDS);
        throw new ImmediateAcknowledgeAmqpException("Confirmed retry publish; ACK original");
    }
}
