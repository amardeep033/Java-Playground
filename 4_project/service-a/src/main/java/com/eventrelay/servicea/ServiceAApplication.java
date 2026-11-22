package com.eventrelay.servicea;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// HTTP REST --- customer events + UI APIs --- A controller method matching the request URL, after the filters run
// RabbitMQ --- final status updates from B --- delivery/messaging/StatusListener.receive()
// Shared 2-thread scheduler pool(common/config/RabbitMqConfig.taskScheduler()) --- Outbox publishing + reconciliation --- delivery/outbox/Outbox.publishPending() + delivery/reconciliation/ReconciliationWorker.reconcile()

@SpringBootApplication
public class ServiceAApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceAApplication.class, args);
    }
}

// spring-boot-starter-web  ---  REST APIs, embedded Tomcat, JSON handling, and REST calls to Service B
// spring-boot-starter-security  ---  Request authentication/authorization, CSRF protection, and password hashing
// spring-security-oauth2-jose  ---  Sign and verify dashboard access JWTs
// spring-boot-starter-validation  ---  Validate request fields using annotations such as `@NotBlank`, `@Email`, and `@Valid`
// spring-boot-starter-test  ---  Testing support, including JUnit, Mockito, AssertJ, and Spring test utilities
// postgresql  ---  JDBC driver connecting the application to PostgreSQL
// spring-boot-starter-data-jpa  ---  Database access through JPA repositories/Hibernate; also brings JDBC support used by `JdbcTemplate`
// spring-boot-starter-amqp  ---  RabbitMQ messaging: publish outbox messages and consume delivery-status updates
