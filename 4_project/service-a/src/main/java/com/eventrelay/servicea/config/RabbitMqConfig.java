package com.eventrelay.servicea.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class RabbitMqConfig {
    public static final String DELIVERY_EXCHANGE = "event-relay.delivery";
    public static final String RETRY_EXCHANGE = "event-relay.retry";
    public static final String STATUS_EXCHANGE = "event-relay.status";
    public static final String DELIVERY_QUEUE = "event-relay.delivery.main";
    public static final String STATUS_QUEUE = "event-relay.status.main";

    @Bean Declarables topology(@Value("${app.retry-delay-ms:30000}") int retryDelay) {
        var delivery = new DirectExchange(DELIVERY_EXCHANGE, true, false);
        var retry = new DirectExchange(RETRY_EXCHANGE, true, false);
        var status = new DirectExchange(STATUS_EXCHANGE, true, false);
        var dead = new DirectExchange("event-relay.dead", true, false);
        var main = QueueBuilder.durable(DELIVERY_QUEUE).deadLetterExchange(dead.getName()).deadLetterRoutingKey("delivery.dead").build();
        var delayed = QueueBuilder.durable("event-relay.delivery.retry").ttl(retryDelay)
                .deadLetterExchange(delivery.getName()).deadLetterRoutingKey("delivery.requested").build();
        var dlq = QueueBuilder.durable("event-relay.delivery.dlq").build();
        var updates = QueueBuilder.durable(STATUS_QUEUE).deadLetterExchange(dead.getName()).deadLetterRoutingKey("status.dead").build();
        var statusDlq = QueueBuilder.durable("event-relay.status.dlq").build();
        return new Declarables(delivery, retry, status, dead, main, delayed, dlq, updates, statusDlq,
                BindingBuilder.bind(main).to(delivery).with("delivery.requested"),
                BindingBuilder.bind(delayed).to(retry).with("delivery.retry"),
                BindingBuilder.bind(dlq).to(dead).with("delivery.dead"),
                BindingBuilder.bind(updates).to(status).with("delivery.updated"),
                BindingBuilder.bind(statusDlq).to(dead).with("status.dead"));
    }
    @Bean ThreadPoolTaskScheduler taskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("outbox-poller-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        return scheduler;
    }
}
