package com.eventrelay.servicea.ingress;

import com.eventrelay.servicea.common.dto.PageResult;
import com.eventrelay.servicea.ingress.dto.IncomingRequestEntry;
import com.eventrelay.servicea.common.search.ListSearch;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Queries joins event data, apply audit-history filters, and read/write PostgreSQL jsonb. Writing that SQL directly is a reasonable design choice.
// We could implement it with JPA, but we would need an entity and repository mapping. JdbcTemplate already handles this persistence work, so JPA is not required.

@Repository
public class IncomingRequestStore {
    private final JdbcTemplate db;
    private final ObjectMapper mapper;

    public IncomingRequestStore(JdbcTemplate db, ObjectMapper mapper) {
        this.db = db;
        this.mapper = mapper;
    }

    //-------------------------------------------------------------------------

    //DB write1: Insert initial audit record (PROCESSING)
    @Transactional(propagation = Propagation.REQUIRES_NEW) //start new transaction -- If the caller later rolls back, the committed audit row survives.
    public void begin(IncomingRequestEntry entry, String presented_key) {
        UUID requested_app_id = null;
        try {
            requested_app_id = UUID.fromString(entry.requestedApplicationId());
        } catch (IllegalArgumentException ignored) {
        }
        UUID key_id = null;
        String key_name = null;
        if (requested_app_id != null && presented_key != null && presented_key.matches("er_[A-Za-z0-9_-]{43}")) {
            String hash;
            try {
                hash = HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(presented_key.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException(impossible);
            }

            var keys = db.queryForList("SELECT id,name FROM service_a.api_keys WHERE application_id=? AND key_hash=?",
                    requested_app_id, hash);
            if (!keys.isEmpty()) {
                key_id = (UUID) keys.getFirst().get("id");
                key_name = (String) keys.getFirst().get("name");
            }
        }
        db.update(
                """
                        INSERT INTO service_a.incoming_requests
                        (id,application_id,requested_application_id,received_at,duration_ms,http_status,outcome,reason_code,reason,auth_method,idempotency_key_present,client_ip,api_key_id,api_key_name)
                        VALUES (?,(SELECT id FROM service_a.applications WHERE id=?),?,?,0,102,'PROCESSING','PROCESSING','Request is being processed',?,?,?,?,?)
                        """,
                entry.id(), requested_app_id, entry.requestedApplicationId(), Timestamp.from(entry.receivedAt()),
                entry.authMethod(), entry.idempotencyKeyPresent(), entry.clientIp(), key_id, key_name);
    }

    // ----------------------------------------------------------------------------

    // Update audit record with final outcome
    @Transactional(propagation = Propagation.REQUIRES_NEW) //If the caller later rolls back, the committed audit row survives.
    public void append(IncomingRequestEntry entry) {
        try {
            db.update(
                    """
                            UPDATE service_a.incoming_requests SET duration_ms=?,http_status=?,outcome=?,reason_code=?,reason=?,event_type=?,skipped_endpoints=?::jsonb,duplicate_of_id=? WHERE id=?
                            """,
                    entry.durationMs(), entry.httpStatus(), entry.outcome(), entry.reasonCode(), entry.reason(),
                    entry.eventType(), mapper.writeValueAsString(entry.skippedEndpoints()), entry.duplicateOfId(),
                    entry.id());
        } catch (JsonProcessingException invalid) {
            throw new IllegalStateException("Unable to encode endpoint audit IDs");
        }
    }

    // ----------------------------------------------------------------------------

    // Fetch audit history without modifying data
    private static final String FROM = " FROM service_a.incoming_requests r LEFT JOIN service_a.received_events e ON e.incoming_request_id=coalesce(r.duplicate_of_id,r.id)";

    @Transactional(readOnly = true)
    public PageResult<IncomingRequestEntry> list(UUID app_id, String outcome, String q, String api_key_name,
            String auth_method, Integer http_status, int page, int size) {
        var outcomes = ListSearch.choices(outcome, Set.of("PROCESSING", "ACCEPTED", "REJECTED", "ERROR"));
        var credentials = ListSearch.choices(auth_method, Set.of("API_KEY", "DASHBOARD", "NONE"));
        if (page < 0 || size < 1 || size > 100 || q.length() > 200 || api_key_name.length() > 120
                || (http_status != null && (http_status < 100 || http_status > 599)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request-history filters or pagination");
        String where = " WHERE true";
        List<Object> args = new ArrayList<>();
        if (app_id != null) {
            where += " AND r.application_id=?";
            args.add(app_id);
        }
        if (!outcomes.isEmpty()) {
            where += " AND r.outcome IN (" + String.join(",", Collections.nCopies(outcomes.size(), "?")) + ")";
            args.addAll(outcomes);
        }
        if (!api_key_name.isBlank()) {
            where += " AND strpos(lower(coalesce(r.api_key_name,'')),lower(?))>0";
            args.add(api_key_name);
        }
        if (!credentials.isEmpty()) {
            where += " AND r.auth_method IN (" + String.join(",", Collections.nCopies(credentials.size(), "?")) + ")";
            args.addAll(credentials);
        }
        if (http_status != null) {
            where += " AND r.http_status=?";
            args.add(http_status);
        }
        if (!q.isBlank()) {
            where += " AND (strpos(lower(r.id::text),lower(?))>0 OR strpos(lower(coalesce(r.event_type,'')),lower(?))>0 OR strpos(lower(r.reason_code),lower(?))>0 OR strpos(lower(r.reason),lower(?))>0 OR strpos(lower(r.requested_application_id),lower(?))>0 OR strpos(r.http_status::text,?)>0 OR strpos(lower(coalesce(r.api_key_name,'')),lower(?))>0 OR strpos(lower(coalesce(e.id::text,'')),lower(?))>0 OR strpos(lower(r.auth_method),lower(?))>0 OR strpos(lower(r.client_ip),lower(?))>0)";
            for (int i = 0; i < 10; i++)
                args.add(q);
        }
        long total = db.queryForObject("SELECT count(*)" + FROM + where, Long.class, args.toArray());
        args.add(size);
        args.add((long) page * size);
        var rows = db.query("SELECT r.*,CASE WHEN r.outcome='ACCEPTED' THEN e.id END AS event_id" + FROM + where
                + " ORDER BY r.received_at DESC,r.id DESC LIMIT ? OFFSET ?", (rs, n) -> {
                    try {
                        return new IncomingRequestEntry(rs.getObject("id", UUID.class),
                                rs.getObject("application_id", UUID.class), rs.getString("requested_application_id"),
                                rs.getTimestamp("received_at").toInstant(), rs.getLong("duration_ms"),
                                rs.getInt("http_status"), rs.getString("outcome"),
                                rs.getString("reason_code"), rs.getString("reason"),
                                rs.getObject("event_id", UUID.class), rs.getString("event_type"),
                                rs.getString("auth_method"), rs.getBoolean("idempotency_key_present"),
                                rs.getString("client_ip"),
                                mapper.readValue(rs.getString("skipped_endpoints"), new TypeReference<List<UUID>>() {
                                }), rs.getObject("api_key_id", UUID.class), rs.getString("api_key_name"),
                                rs.getObject("duplicate_of_id", UUID.class));
                    } catch (JsonProcessingException invalid) {
                        throw new IllegalStateException("Invalid endpoint audit IDs");
                    }
                }, args.toArray());
        return new PageResult<>(rows, page, size, total, (int) ((total + size - 1) / size));
    }
}

// 1. With Hibernate (Spring Data JPA): requires @Entity - extends JpaRepository - (UserAccountRepository)
// 1.1 Built-in        -- findById(), save(), deleteById()
// 1.2 Derived         -- findByEmail(), findByNameAndStatus()
// 1.3 @Query JPQL     -- @Query("SELECT u FROM UserAccount u") [Entity → SQL]
// 1.4 @Query Native   -- @Query(value = "SELECT * FROM users", nativeQuery = true) [Direct SQL]

// 2. Without Hibernate (Spring JDBC): no @Entity required - @Repository, JdbcTemplate, ObjectMapper - precise control needed (IncomingRequestStore)
// 2.1 JdbcTemplate    -- db.query("SELECT * FROM users", rowMapper)
// 2.2 JdbcTemplate    -- db.update("INSERT INTO users ...", params)

