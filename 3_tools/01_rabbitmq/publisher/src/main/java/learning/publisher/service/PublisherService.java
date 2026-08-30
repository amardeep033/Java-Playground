package learning.publisher.service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import learning.publisher.config.RabbitMqConfig;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class PublisherService {
    private final RabbitTemplate rabbit;

    public PublisherService(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public String publish(String text, String messageId) throws Exception {

        String id = messageId == null || messageId.isBlank() ? UUID.randomUUID().toString() : messageId;
        Message message = MessageBuilder.withBody(text.getBytes(StandardCharsets.UTF_8))
                                        .setContentType("text/plain")
                                        .setContentEncoding("UTF-8") // Encoding metadata
                                        .setDeliveryMode(MessageDeliveryMode.PERSISTENT) // Messages can survive broker restarts when durability requirements are met. Each setting protects a different entity: exchange, queue, or message. Durability at one level does not make the others durable.
                                        .setMessageId(id) // For tracking, debugging, and application-managed deduplication
                                        .build();

        CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());

        // 1. exchange: string -- selects the exchange whose bindings determine the destination queues.
        // 2. routingKey: string -- matched against the selected topic exchange's binding patterns.
        // 3. message: The body is a byte array (byte[]); RabbitMQ does not require JSON. You can send JSON, plain text, binary data, or other formats. Publisher and consumer must agree on encoding and decoding.
        // 4. correlationData: tracks this publication's confirm/return; the message correlationId property links requests, replies, or workflows.
        rabbit.send(RabbitMqConfig.EXCHANGE, RabbitMqConfig.MAIN_KEY, message, correlation);

        // 0.1 In application.properties, publisher-confirm-type=correlated enables the confirm future -- condition 1.
        // 0.2 In application.properties, publisher-returns=true and template.mandatory=true enable returns for unroutable messages -- condition 2.
        // 1. Condition 1: !isAck() detects a publisher NACK. Wait at most 5 seconds; if the future does not complete, get() throws TimeoutException rather than returning false.
        // 2. Condition 2: a publisher ACK does not mean a queue matched. Check whether RabbitMQ returned the message because it could not route it to a queue.
        if (!correlation.getFuture().get(5, TimeUnit.SECONDS).isAck() || correlation.getReturned() != null) {
            throw new AmqpException("Publish was rejected or unroutable");
        }
        
        return id;
    }
}

// Identifier	Purpose
// messageId	Application-provided message identifier
// Delivery tag	RabbitMQ identifier for a particular delivery on a channel, used for ACK/NACK
// Correlation ID	Often used to associate a message with a request or workflow

// Exchange	Queue	Message	Result after broker restart
// Durable	Durable	Persistent	Intended to survive, subject to broker durability guarantees
// Durable	Durable	Non-persistent	Queue survives, but the message is not guaranteed to survive
// Non-durable	Durable	Persistent	Queue and message may survive, but the exchange will not survive
// Durable	Non-durable	Persistent	The queue will not survive, so the message cannot be recovered from that queue

// The core distinction
// - Publisher confirm: Did RabbitMQ acknowledge responsibility for the publication?
// - Publisher return: Was the message unroutable and returned to the publisher?
// You need both checks because a message can receive a publisher ACK and still be unroutable.
// Publisher confirm	Returned message?	Throws exception?
// ACK	No	No
// ACK	Yes	Yes
// NACK	No	Yes
// NACK	Yes	Yes

// Question     correlationId	CorrelationData
// What is it?	Message metadata	Spring AMQP Java object
// Purpose	Link related messages	Track a particular publish attempt
// Contains publisher ACK/NACK?	No	Yes, through its confirm future
// Tracks returned message?	No	Yes, when publisher returns are configured
// Example	"order-123"	new CorrelationData("publish-456")