package com.eventrelay.servicea.delivery.messaging;

import com.eventrelay.servicea.common.config.RabbitMqConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class StatusListener {
    private final ObjectMapper mapper;
    private final StatusUpdateService service;

    public StatusListener(ObjectMapper mapper, StatusUpdateService service) {
        this.mapper = mapper;
        this.service = service;
    }

    // listens from service B to service A msgs
    @RabbitListener(queues = RabbitMqConfig.STATUS_QUEUE)
    public void receive(Message message, Channel channel) throws IOException {

        long tag = message.getMessageProperties().getDeliveryTag();
        DeliveryStatusMessage update;
        try {
            if (message.getBody().length > 524288)
                throw new IllegalArgumentException("Status message is too large");
            
            update = mapper.readValue(message.getBody(), DeliveryStatusMessage.class);
            if (update == null)
                throw new IllegalArgumentException("Empty message");

            update.validate();
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            // reject and dont retry -- pass to dlq
            channel.basicReject(tag, false);
            return;
        }
        

        try {
            // apply update and then acknowledge the msg
            service.apply(update);
            channel.basicAck(tag, false);
        } catch (IllegalArgumentException ex) {
            // reject and dont retry -- pass to dlq
            channel.basicReject(tag, false);
        } catch (RuntimeException ex) {
            // temporary service a issue like db connection -- so dont retry instantly -- nack after some time -- so rabbitmq dont send back instantly
            try {
                Thread.sleep(1000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            // requeue true means -- no dlq -- first retry (infinite times -- fix it using status.retry queue???) 
            channel.basicNack(tag, false, true);
            // channel.basicReject(tag, true); //same effect
        }
    }
}

// basicReject → Reject one message only.
// basicNack → Reject one or multiple messages.
