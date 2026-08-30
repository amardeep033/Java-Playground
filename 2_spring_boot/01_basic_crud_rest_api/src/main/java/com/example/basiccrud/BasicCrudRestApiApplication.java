package com.example.basiccrud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// MVC flow: Controller handles HTTP, Service holds business logic, Repository handles data storage.
// Port: configured in resources/application.properties using server.port=8080; Spring Boot reads it during startup.
// Both the file name application.properties and the key server.port are Spring Boot standards, so they are auto-detected.

@SpringBootApplication // Equivalent to @Configuration + @EnableAutoConfiguration + @ComponentScan.
public class BasicCrudRestApiApplication {

    public static void main(String[] args) {
        // Create ApplicationContext, configure Spring, scan components, and start the embedded server.
        SpringApplication.run(BasicCrudRestApiApplication.class, args); // First arg is the main configuration class; second arg is CLI arguments.
    }
}

// pom.xml dependency/plugin                 | What it does / why we use it
// ------------------------------------------|------------------------------------------------------------
// spring-boot-starter-web                   | Adds Spring MVC, REST annotations, JSON conversion, and embedded Tomcat.
// spring-boot-starter-validation            | Adds @Valid, @NotBlank, @PositiveOrZero for request validation.
// spring-boot-starter-test                  | Adds testing tools like JUnit, Spring Boot Test, Mockito, and assertions.
// spring-boot-maven-plugin                  | Lets Maven run/package the app using commands like mvn spring-boot:run.
// org.springframework.boot.SpringApplication| Java import used to start the Spring app, create context, and start the server.