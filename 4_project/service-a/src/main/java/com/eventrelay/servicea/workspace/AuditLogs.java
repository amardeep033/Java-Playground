package com.eventrelay.servicea.workspace;

import com.eventrelay.servicea.auth.JwtUser;
import com.eventrelay.servicea.auth.access.WorkspaceAccess;
import com.eventrelay.servicea.common.search.ListSearch;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Successful changes only, committed atomically with their mutation. Never pass credential values here. */
@Service
public class AuditLogs {
    private final JdbcTemplate db;
    private final ObjectMapper mapper;
    public AuditLogs(JdbcTemplate db,ObjectMapper mapper) { this.db=db;this.mapper=mapper; }
    public void changed(UUID app_id,String entity,UUID resource_id,String field,Object before,Object after) {
        if(!Set.of("name","email","role","is_active","url","eventTypeIds","mapped").contains(field))
            throw new IllegalArgumentException("This field cannot have audit values");
        if(Objects.equals(before,after)) return;
        write(app_id,entity,resource_id,"UPDATE",Map.of("field",field,"old",before==null?"":before,"new",after==null?"":after));
    }
    public void write(UUID app_id,String entity,UUID resource_id,String action,Map<String,?> details) {
        rejectSecrets(mapper.valueToTree(details));
        UUID client_id=app_id==null?db.queryForObject("SELECT client_id FROM service_a.users WHERE id=?",UUID.class,JwtUser.current().id()):db.queryForObject("SELECT client_id FROM service_a.applications WHERE id=?",UUID.class,app_id);
        writeScope(app_id,client_id,entity,resource_id,action,details);
    }
    public void writeClient(UUID client_id,String entity,UUID resource_id,String action,Map<String,?> details) {
        writeScope(null,client_id,entity,resource_id,action,details);
    }
    private void writeScope(UUID app_id,UUID client_id,String entity,UUID resource_id,String action,Map<String,?> details) {
        var actor=JwtUser.current();
        if(actor==null) throw new IllegalStateException("Audit mutation requires an authenticated actor");
        // Fail closed if a caller accidentally supplies credential fields, even nested ones.
        JsonNode safe_details = mapper.valueToTree(details);
        rejectSecrets(safe_details);
        try {
            db.update("INSERT INTO service_a.audit_logs(id,application_id,actor_id,entity_type,entity_id,action,details,client_id) VALUES (?,?,?,?,?,?,?::jsonb,?)",
                    UUID.randomUUID(),app_id,actor.id(),entity,resource_id,action,mapper.writeValueAsString(safe_details),client_id);
        } catch(JsonProcessingException e) { throw new IllegalStateException("Cannot encode audit metadata"); }
    }
    private void rejectSecrets(JsonNode node) {
        if (node.isObject()) {
            for (var entry : node.properties()) {
                String audit_field_name = entry.getKey().replaceAll("[^a-zA-Z]", "").toLowerCase(Locale.ROOT);
                if (audit_field_name.contains("password") || audit_field_name.contains("secret")
                        || audit_field_name.contains("token") || audit_field_name.contains("keyhash")
                        || audit_field_name.equals("apikey") || audit_field_name.equals("authorization")) {
                    throw new IllegalArgumentException("Credentials cannot be audit values");
                }
                rejectSecrets(entry.getValue());
            }
        } else if (node.isArray()) {
            node.forEach(this::rejectSecrets);
        }
    }
    /** One company-admin read API; filters apply in SQL before pagination. */
    public Map<String,Object> listClient(UUID client_id,String resources,String actions,String actors,String scopes,String sort,String q,int page,int size) {
        WorkspaceAccess.validatePage(q,page,size);
        if(!Set.of("asc","desc").contains(sort)) throw badFilter();
        String resource="CASE WHEN l.entity_type IN ('users','workspace_members','clients') THEN 'user' WHEN l.entity_type='applications' THEN 'app' WHEN l.entity_type='api_keys' THEN 'apikey' WHEN l.entity_type='event_types' THEN 'eventtype' ELSE 'endpoint' END";
        String action="CASE WHEN l.action IN ('CREATED','USER_CREATED','CREATE') THEN 'create' WHEN l.action IN ('DELETED','DELETE') THEN 'delete' ELSE 'update' END";
        String where="l.client_id=?";
        var args=new ArrayList<Object>();args.add(client_id);
        where+=filter(resource,resources,Set.of("user","app","apikey","endpoint","eventtype"),args);
        where+=filter(action,actions,Set.of("create","update","delete"),args);
        var actor_ids=values(actors);
        for(String actor:actor_ids) uuid(actor);
        if(!actor_ids.isEmpty()) {where+=" AND l.actor_id::text IN ("+placeholders(actor_ids.size())+")";args.addAll(actor_ids);}
        var scope_ids=values(scopes);
        if(!scope_ids.isEmpty()) {
            var app_ids=new ArrayList<String>();boolean overall=scope_ids.contains("overall");
            for(String scope:scope_ids) if(!scope.equals("overall")) {uuid(scope);app_ids.add(scope);}
            where+=" AND ("+(overall?"l.application_id IS NULL":"FALSE");
            if(!app_ids.isEmpty()) {where+=" OR l.application_id::text IN ("+placeholders(app_ids.size())+")";args.addAll(app_ids);}
            where+=")";
        }
        where+=" AND (lower(l.entity_type) LIKE ? ESCAPE '!' OR lower(l.action) LIKE ? ESCAPE '!' OR lower(l.details::text) LIKE ? ESCAPE '!' OR lower(coalesce(u.name,'')) LIKE ? ESCAPE '!' OR lower(coalesce(a.name,'')) LIKE ? ESCAPE '!')";
        String pattern=ListSearch.pattern(q);for(int i=0;i<5;i++)args.add(pattern);
        String from=" FROM service_a.audit_logs l LEFT JOIN service_a.users u ON u.id=l.actor_id AND u.client_id=l.client_id LEFT JOIN service_a.applications a ON a.id=l.application_id AND a.client_id=l.client_id WHERE "+where;
        long count=db.queryForObject("SELECT count(*)"+from,Long.class,args.toArray());
        args.add(size);args.add((long)page*size);
        var rows=db.queryForList("SELECT l.*,u.name AS actor_name,a.name AS application_name,"+resource+" AS resource,"+action+" AS display_action"+from+" ORDER BY l.created_at "+sort+",l.id "+sort+" LIMIT ? OFFSET ?",args.toArray());
        rows.forEach(row->{try {row.put("details",mapper.readTree(row.get("details").toString()));}catch(JsonProcessingException e){throw new IllegalStateException("Invalid audit metadata");}});
        // Company catalogs are small; avoid rescanning audit history for filter options.
        // Include soft-deleted users/apps so historical entries remain filterable.
        var actor_options=db.queryForList("SELECT id::text AS value,name AS label FROM service_a.users WHERE client_id=? ORDER BY label,value",client_id);
        var app_options=db.queryForList("SELECT id::text AS value,name AS label FROM service_a.applications WHERE client_id=? ORDER BY label,value",client_id);
        return Map.of("items",rows,"page",page,"size",size,"totalElements",count,"totalPages",(count+size-1)/size,"actors",actor_options,"scopes",app_options);
    }
    private List<String> values(String input) {
        if(input==null||input.isBlank())return List.of();
        if(input.length()>8000)throw badFilter();
        var result=Arrays.stream(input.split(",")).map(String::trim).distinct().toList();
        if(result.size()>100||result.contains(""))throw badFilter();return result;
    }
    private String filter(String expression,String input,Set<String> allowed,List<Object> args) {
        var values=values(input);if(!allowed.containsAll(values))throw badFilter();
        if(values.isEmpty())return "";args.addAll(values);return " AND "+expression+" IN ("+placeholders(values.size())+")";
    }
    private String placeholders(int count) {return String.join(",",Collections.nCopies(count,"?"));}
    private void uuid(String value) {try {UUID.fromString(value);}catch(IllegalArgumentException e){throw badFilter();}}
    private ResponseStatusException badFilter() {return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid audit filter");}
}
