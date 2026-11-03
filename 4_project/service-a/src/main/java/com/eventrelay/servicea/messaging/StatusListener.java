package com.eventrelay.servicea.messaging;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.eventrelay.servicea.config.RabbitMqConfig;

@Component
public class StatusListener {
    private final ObjectMapper mapper;
    private final StatusUpdateService service;
    public StatusListener(ObjectMapper mapper, StatusUpdateService service) { this.mapper = mapper; this.service = service; }
    @RabbitListener(queues = RabbitMqConfig.STATUS_QUEUE)
    public void receive(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        DeliveryStatusMessage update;
        try {
            if (message.getBody().length > 65536) throw new IllegalArgumentException("Status message is too large");
            update = mapper.readValue(message.getBody(), DeliveryStatusMessage.class); if (update == null) throw new IllegalArgumentException("Empty message"); update.validate();
        } catch (JsonProcessingException | IllegalArgumentException ex) { channel.basicReject(tag, false); return; }
        try { service.apply(update); channel.basicAck(tag, false); }
        catch (IllegalArgumentException ex) { channel.basicReject(tag, false); }
        catch (RuntimeException ex) {
            try { Thread.sleep(1000); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            channel.basicNack(tag, false, true);
        }
    }
}
