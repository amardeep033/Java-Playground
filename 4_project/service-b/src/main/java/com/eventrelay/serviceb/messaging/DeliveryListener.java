package com.eventrelay.serviceb.messaging;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.eventrelay.serviceb.config.RabbitMqConfig;
import com.eventrelay.serviceb.delivery.DeliveryService;

@Component
public class DeliveryListener {
    private final ObjectMapper mapper;
    private final DeliveryService service;
    public DeliveryListener(ObjectMapper mapper, DeliveryService service) { this.mapper = mapper; this.service = service; }
    @RabbitListener(queues = RabbitMqConfig.DELIVERY_QUEUE)
    public void receive(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        DeliveryCommand command;
        try {
            if (message.getBody().length > 2_000_000) throw new IllegalArgumentException("Delivery command too large");
            command = mapper.readValue(message.getBody(), DeliveryCommand.class); if (command == null) throw new IllegalArgumentException("Empty message"); command.validate();
        } catch (JsonProcessingException | IllegalArgumentException ex) { channel.basicReject(tag, false); return; }
        try {
            boolean dead = service.process(command);
            if (dead) channel.basicReject(tag, false); else channel.basicAck(tag, false);
        } catch (IllegalArgumentException ex) { channel.basicReject(tag, false); }
        catch (RuntimeException ex) {
            try { Thread.sleep(1000); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            channel.basicNack(tag, false, true);
        }
    }
}
