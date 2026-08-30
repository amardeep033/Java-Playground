package learning.publisher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// spring-boot-starter-web: handles HTTP API requests.
// spring-boot-starter-amqp: supports publishing RabbitMQ messages.

@SpringBootApplication
public class PublisherApplication {
    public static void main(String[] args) {
        SpringApplication.run(PublisherApplication.class, args);
    }
}
