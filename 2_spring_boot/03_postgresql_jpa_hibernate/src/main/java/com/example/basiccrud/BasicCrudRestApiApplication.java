package com.example.basiccrud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// pom.xml adds spring-boot-starter-data-jpa and the PostgreSQL JDBC driver.
// 1. Product.java changes from a plain model class into a JPA entity mapped to the products table.
// 2. ProductRepository.java changes from a hand-written in-memory repository into a Spring Data JPA repository interface.
// 3. DataSeeder.java replaces constructor-based in-memory seed data with startup database seed data.
// Controller, DTO, and exception classes intentionally stay almost the same, because persistence is hidden behind the service/repository boundary.

@SpringBootApplication
public class BasicCrudRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicCrudRestApiApplication.class, args);
    }
}

// Java objects <-> Object-Relational Mapping (ORM) <-> database tables
// You don't write SQL, create a JDBC connection or create a PreparedStatement.
// DataSource: Spring Boot creates the database connection pool from spring.datasource.* properties.
// findById is provided by Spring Data JPA.
// JPA provides concepts such as Entity, EntityManager, Persistence Context, Query, and Transaction.
// Hibernate uses entity mapping metadata to construct and execute the appropriate SQL.

// Your code
//    ↓
// Spring Data JPA - creates the repository implementation at runtime from ProductRepository extends JpaRepository<Product, Long>
//    ↓
// JPA - specification that defines how Java applications map objects to relational databases
//    ↓
// Hibernate - JPA implementation that converts Product objects to SQL and SQL rows back to Product objects
//    ↓
// JDBC - Java's lower-level database API: connection, PreparedStatement, ResultSet
//    ↓
// PostgreSQL
