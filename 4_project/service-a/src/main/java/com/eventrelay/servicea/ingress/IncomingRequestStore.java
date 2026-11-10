package com.eventrelay.servicea.ingress;

import java.time.*;
import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.eventrelay.servicea.dto.Responses.PageResult;

@Service
public class IncomingRequestStore {
    private final JdbcTemplate db;
    private final ObjectMapper mapper;
    public IncomingRequestStore(JdbcTemplate db,ObjectMapper mapper) { this.db=db;this.mapper=mapper; }
    public record Entry(UUID id,UUID applicationId,String requestedApplicationId,Instant receivedAt,long durationMs,
                        int httpStatus,String outcome,String reasonCode,String reason,UUID eventId,String eventType,
                        String authMethod,boolean idempotencyKeyPresent,String clientIp,List<UUID> skippedEndpoints,UUID apiKeyId,String apiKeyName,UUID duplicateOfId) {}
    // The audit row commits first; accepted events reference it. Credential values never persist.
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void begin(Entry entry,String presentedKey) {
        UUID target=null;
        try { target=UUID.fromString(entry.requestedApplicationId()); } catch(IllegalArgumentException ignored) {}
        UUID keyId=null;String keyName=null;
        if(target!=null && presentedKey!=null && presentedKey.matches("er_[A-Za-z0-9_-]{43}")) {
            String hash;
            try { hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(presentedKey.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
            catch(java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
            // Attribution only, never authentication; scope prevents exposing another app's key name.
            // Revoked/deleted key labels remain useful when their submission is rejected.
            var keys=db.queryForList("SELECT id,name FROM service_a.api_keys WHERE application_id=? AND key_hash=?",target,hash);
            if(!keys.isEmpty()) { keyId=(UUID)keys.getFirst().get("id");keyName=(String)keys.getFirst().get("name"); }
        }
        db.update("""
            INSERT INTO service_a.incoming_requests
            (id,application_id,requested_application_id,received_at,duration_ms,http_status,outcome,reason_code,reason,auth_method,idempotency_key_present,client_ip,api_key_id,api_key_name)
            VALUES (?,(SELECT id FROM service_a.applications WHERE id=?),?,?,0,102,'PROCESSING','PROCESSING','Request is being processed',?,?,?,?,?)
            """,entry.id(),target,entry.requestedApplicationId(),java.sql.Timestamp.from(entry.receivedAt()),entry.authMethod(),entry.idempotencyKeyPresent(),entry.clientIp(),keyId,keyName);
    }
    // Independent completion persists rejection after the ingestion transaction rolls back.
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void append(Entry entry) {
        try {
            db.update("""
                UPDATE service_a.incoming_requests SET duration_ms=?,http_status=?,outcome=?,reason_code=?,reason=?,event_type=?,skipped_endpoints=?::jsonb,duplicate_of_id=? WHERE id=?
                """,entry.durationMs(),entry.httpStatus(),entry.outcome(),entry.reasonCode(),entry.reason(),entry.eventType(),mapper.writeValueAsString(entry.skippedEndpoints()),entry.duplicateOfId(),entry.id());
        } catch(JsonProcessingException invalid) { throw new IllegalStateException("Unable to encode endpoint audit IDs"); }
    }
    private static final String FROM=" FROM service_a.incoming_requests r LEFT JOIN service_a.received_events e ON e.incoming_request_id=coalesce(r.duplicate_of_id,r.id)";
    @Transactional(readOnly=true)
    public PageResult<Entry> list(UUID app,String outcome,String q,String apiKeyName,String authMethod,Integer httpStatus,int page,int size) {
        var outcomes=com.eventrelay.servicea.service.ListSearch.choices(outcome,Set.of("PROCESSING","ACCEPTED","REJECTED","ERROR"));
        var credentials=com.eventrelay.servicea.service.ListSearch.choices(authMethod,Set.of("API_KEY","DASHBOARD","NONE"));
        if(page<0||size<1||size>100||q.length()>200||apiKeyName.length()>120||(httpStatus!=null&&(httpStatus<100||httpStatus>599)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid request-history filters or pagination");
        String where=" WHERE true";
        List<Object> args=new ArrayList<>();
        if(app!=null) { where+=" AND r.application_id=?";args.add(app); }
        if(!outcomes.isEmpty()) {where+=" AND r.outcome IN ("+String.join(",",Collections.nCopies(outcomes.size(),"?"))+")";args.addAll(outcomes);}
        if(!apiKeyName.isBlank()) { where+=" AND strpos(lower(coalesce(r.api_key_name,'')),lower(?))>0";args.add(apiKeyName); }
        if(!credentials.isEmpty()) {where+=" AND r.auth_method IN ("+String.join(",",Collections.nCopies(credentials.size(),"?"))+")";args.addAll(credentials);}
        if(httpStatus!=null) { where+=" AND r.http_status=?";args.add(httpStatus); }
        if(!q.isBlank()) {
            where+=" AND (strpos(lower(r.id::text),lower(?))>0 OR strpos(lower(coalesce(r.event_type,'')),lower(?))>0 OR strpos(lower(r.reason_code),lower(?))>0 OR strpos(lower(r.reason),lower(?))>0 OR strpos(lower(r.requested_application_id),lower(?))>0 OR strpos(r.http_status::text,?)>0 OR strpos(lower(coalesce(r.api_key_name,'')),lower(?))>0 OR strpos(lower(coalesce(e.id::text,'')),lower(?))>0 OR strpos(lower(r.auth_method),lower(?))>0 OR strpos(lower(r.client_ip),lower(?))>0)";
            for(int i=0;i<10;i++) args.add(q);
        }
        long total=db.queryForObject("SELECT count(*)"+FROM+where,Long.class,args.toArray());
        args.add(size);args.add((long)page*size);
        var rows=db.query("SELECT r.*,CASE WHEN r.outcome='ACCEPTED' THEN e.id END AS event_id"+FROM+where+" ORDER BY r.received_at DESC,r.id DESC LIMIT ? OFFSET ?",(rs,n)-> {
            try {
                return new Entry(rs.getObject("id",UUID.class),rs.getObject("application_id",UUID.class),rs.getString("requested_application_id"),
                        rs.getTimestamp("received_at").toInstant(),rs.getLong("duration_ms"),rs.getInt("http_status"),rs.getString("outcome"),
                        rs.getString("reason_code"),rs.getString("reason"),rs.getObject("event_id",UUID.class),rs.getString("event_type"),
                        rs.getString("auth_method"),rs.getBoolean("idempotency_key_present"),rs.getString("client_ip"),
                        mapper.readValue(rs.getString("skipped_endpoints"),new TypeReference<List<UUID>>() {}),rs.getObject("api_key_id",UUID.class),rs.getString("api_key_name"),rs.getObject("duplicate_of_id",UUID.class));
            } catch(JsonProcessingException invalid) { throw new IllegalStateException("Invalid endpoint audit IDs"); }
        },args.toArray());
        return new PageResult<>(rows,page,size,total,(int)((total+size-1)/size));
    }
}
