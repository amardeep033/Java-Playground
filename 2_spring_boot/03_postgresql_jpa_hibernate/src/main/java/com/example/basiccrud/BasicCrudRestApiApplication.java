package com.example.basiccrud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 03 keeps the same REST + DTO + exception flow from 02, but stores Product rows in PostgreSQL.
// Spring Data JPA creates the repository implementation at runtime; Hibernate converts Product objects to SQL.
// The controller still does not know whether data comes from a map, PostgreSQL, or another storage engine.

@SpringBootApplication
public class BasicCrudRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicCrudRestApiApplication.class, args); // Creates the Spring context and starts embedded Tomcat.
    }
}
