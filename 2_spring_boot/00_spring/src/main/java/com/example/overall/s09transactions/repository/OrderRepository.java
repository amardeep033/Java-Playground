package com.example.overall.s9transactions.repository;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Repository
public class OrderRepository {
    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void createTables() {
        // Create tiny tables at startup so every run starts with an empty in-memory database.
        jdbcTemplate.execute("""
                create table orders (
                    id identity primary key,
                    note varchar(100) not null
                )
                """);
        jdbcTemplate.execute("""
                create table audits (
                    id identity primary key,
                    note varchar(100) not null
                )
                """);
    }

    public void saveOrder(String note) {
        // txActive=true proves this JDBC call is running inside a Spring-managed transaction.
        jdbcTemplate.update("insert into orders(note) values (?)", note);
        System.out.println("R1. insert orders: " + note
                + " txActive=" + TransactionSynchronizationManager.isActualTransactionActive());
    }

    public void saveAudit(String note) {
        // Same physical H2 database, but the transaction boundary depends on who called this method.
        jdbcTemplate.update("insert into audits(note) values (?)", note);
        System.out.println("R2. insert audits: " + note
                + " txActive=" + TransactionSynchronizationManager.isActualTransactionActive());
    }

    public void printRows() {
        System.out.println("DB1. orders=" + rows("orders"));
        System.out.println("DB2. audits=" + rows("audits"));
    }

    private List<String> rows(String table) {
        return jdbcTemplate.queryForList("select note from " + table + " order by id", String.class);
    }
}
