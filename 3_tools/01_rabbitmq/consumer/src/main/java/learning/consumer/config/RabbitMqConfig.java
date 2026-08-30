package learning.consumer.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Declares the same topology as the publisher.
@Configuration
public class RabbitMqConfig {
    public static final String EXCHANGE = "learning.events";

    public static final String MAIN_QUEUE = "learning.main";
    public static final String RETRY_QUEUE = "learning.retry";
    public static final String DLQ_QUEUE = "learning.dlq";

    public static final String MAIN_KEY = "work.main";
    public static final String RETRY_KEY = "work.retry";
    public static final String DLQ_KEY = "work.dlq";

    @Bean
    public Declarables rabbitTopology() {
        TopicExchange exchange = new TopicExchange(EXCHANGE);

        Queue main = QueueBuilder.durable(MAIN_QUEUE)
                                .deadLetterExchange(EXCHANGE)
                                .deadLetterRoutingKey(DLQ_KEY)
                                .build();

        Queue retry = QueueBuilder.durable(RETRY_QUEUE)
                                .ttl(5000)
                                .deadLetterExchange(EXCHANGE)
                                .deadLetterRoutingKey(MAIN_KEY)
                                .build();

        Queue dlq = QueueBuilder.durable(DLQ_QUEUE)
                                .build();

        return new Declarables(exchange, 
                                main, retry, dlq,
                                BindingBuilder.bind(main).to(exchange).with(MAIN_KEY),
                                BindingBuilder.bind(retry).to(exchange).with(RETRY_KEY),
                                BindingBuilder.bind(dlq).to(exchange).with(DLQ_KEY));
    }
}
