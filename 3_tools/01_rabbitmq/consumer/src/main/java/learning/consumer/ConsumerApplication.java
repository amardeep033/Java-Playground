package learning.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// spring-boot-starter-web: not included; this app has no HTTP endpoints.
// spring-boot-starter-amqp: consumes messages and publishes retry copies.

@SpringBootApplication
public class ConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
