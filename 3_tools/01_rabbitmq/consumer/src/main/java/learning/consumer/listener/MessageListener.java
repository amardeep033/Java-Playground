package learning.consumer.listener;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import learning.consumer.config.RabbitMqConfig;
import learning.consumer.service.ConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {
    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);
    private final ConsumerService service;

    public MessageListener(ConsumerService service) {
        this.service = service;
    }

    // We listen only to the main queue.
    // From the producer: body, messageId, correlationId (only if set), and content type/encoding; CorrelationData stays in the publishing app.
    // From RabbitMQ: delivery tag, redelivered flag, received exchange/routing key, and dead-letter headers when applicable.
    // Spring supplies the consumer Channel: a logical session within a connection to RabbitMQ.
    @RabbitListener(queues = RabbitMqConfig.MAIN_QUEUE)

    // Why both text and message? text provides the decoded payload; message provides the original bytes and metadata.
    public void consume(String text, Message message, Channel channel) throws IOException {
        long rbmq_delivery_tag = message.getMessageProperties().getDeliveryTag(); // Delivery number on this channel.
        String prod_msg_id = message.getMessageProperties().getMessageId(); // For idempotency.

        try {
            // Missing IDs are treated as permanent failures and rejected by the catch block.
            if (prod_msg_id == null || prod_msg_id.isBlank()) {
                throw new IllegalArgumentException("Missing message ID");
            }

            // Check idempotency and process; return whether new work completed.
            boolean processed = service.processOnce(prod_msg_id, text);
            
            // ACK after successful processing or when the event was already processed.
            channel.basicAck(rbmq_delivery_tag, false);
            log.info("ACK: id={}, duplicate={}", prod_msg_id, !processed);

        } catch (IllegalArgumentException permanentFailure) { // Does not catch IllegalStateException (temporary failure).
            
            // Reject permanent failures without Spring retries; RabbitMQ routes them through the configured DLX to the DLQ.
            channel.basicReject(rbmq_delivery_tag, false);
            log.info("REJECT -> DLQ: id={}, reason={}", prod_msg_id, permanentFailure.getMessage());
        }

        // Other failures propagate to Spring's listener retry interceptor; RetryRecovery handles exhausted attempts.
    }
}
