package com.eventrelay.servicea.workspace;

import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.eventrelay.servicea.auth.JwtUser;
import com.eventrelay.servicea.service.ListSearch;

/** Successful changes only, committed atomically with their mutation. Never pass credential values here. */
@Service
public class AuditLogs {
    private final JdbcTemplate db;
    private final ObjectMapper mapper;
    public AuditLogs(JdbcTemplate db,ObjectMapper mapper) { this.db=db;this.mapper=mapper; }
    public void changed(UUID app,String entity,UUID id,String field,Object before,Object after) {
        if(!Set.of("name","email","role","is_active","url","eventTypeIds","mapped").contains(field))
            throw new IllegalArgumentException("This field cannot have audit values");
        if(Objects.equals(before,after)) return;
        write(app,entity,id,"UPDATE",Map.of("field",field,"old",before==null?"":before,"new",after==null?"":after));
    }
    public void write(UUID app,String entity,UUID id,String action,Map<String,?> details) {
        rejectSecrets(mapper.valueToTree(details));
        UUID client=app==null?db.queryForObject("SELECT client_id FROM service_a.users WHERE id=?",UUID.class,JwtUser.current().id()):db.queryForObject("SELECT client_id FROM service_a.applications WHERE id=?",UUID.class,app);
        writeScope(app,client,entity,id,action,details);
    }
    public void writeClient(UUID client,String entity,UUID id,String action,Map<String,?> details) {
        writeScope(null,client,entity,id,action,details);
    }
    private void writeScope(UUID app,UUID client,String entity,UUID id,String action,Map<String,?> details) {
        var actor=JwtUser.current();
        if(actor==null) throw new IllegalStateException("Audit mutation requires an authenticated actor");
        // Fail closed if a caller accidentally supplies credential fields, even nested ones.
        var safe=mapper.valueToTree(details);
        rejectSecrets(safe);
        try {
            db.update("INSERT INTO service_a.audit_logs(id,application_id,actor_id,entity_type,entity_id,action,details,client_id) VALUES (?,?,?,?,?,?,?::jsonb,?)",
                    UUID.randomUUID(),app,actor.id(),entity,id,action,mapper.writeValueAsString(safe),client);
        } catch(JsonProcessingException e) { throw new IllegalStateException("Cannot encode audit metadata"); }
    }
    private void rejectSecrets(com.fasterxml.jackson.databind.JsonNode node) {
        if(node.isObject()) node.fields().forEachRemaining(entry->{
            String key=entry.getKey().replaceAll("[^a-zA-Z]","").toLowerCase(Locale.ROOT);
            if(key.contains("password")||key.contains("secret")||key.contains("token")||key.contains("keyhash")||key.equals("apikey")||key.equals("authorization"))
                throw new IllegalArgumentException("Credentials cannot be audit values");
            rejectSecrets(entry.getValue());
        });
        else if(node.isArray()) node.forEach(this::rejectSecrets);
    }
    /** One company-admin read API; filters apply in SQL before pagination. */
    public Map<String,Object> listClient(UUID client,String resources,String actions,String actors,String scopes,String sort,String q,int page,int size) {
        WorkspaceAccess.validatePage(q,page,size);
        if(!Set.of("asc","desc").contains(sort)) throw badFilter();
        String resource="CASE WHEN l.entity_type IN ('users','workspace_members','clients') THEN 'user' WHEN l.entity_type='applications' THEN 'app' WHEN l.entity_type='api_keys' THEN 'apikey' WHEN l.entity_type='event_types' THEN 'eventtype' ELSE 'endpoint' END";
        String action="CASE WHEN l.action IN ('CREATED','USER_CREATED','CREATE') THEN 'create' WHEN l.action IN ('DELETED','DELETE') THEN 'delete' ELSE 'update' END";
        String where="l.client_id=?";
        var args=new ArrayList<Object>();args.add(client);
        where+=filter(resource,resources,Set.of("user","app","apikey","endpoint","eventtype"),args);
        where+=filter(action,actions,Set.of("create","update","delete"),args);
        var actorIds=values(actors);
        for(String actor:actorIds) uuid(actor);
        if(!actorIds.isEmpty()) {where+=" AND l.actor_id::text IN ("+placeholders(actorIds.size())+")";args.addAll(actorIds);}
        var scopeIds=values(scopes);
        if(!scopeIds.isEmpty()) {
            var appIds=new ArrayList<String>();boolean overall=scopeIds.contains("overall");
            for(String scope:scopeIds) if(!scope.equals("overall")) {uuid(scope);appIds.add(scope);}
            where+=" AND ("+(overall?"l.application_id IS NULL":"FALSE");
            if(!appIds.isEmpty()) {where+=" OR l.application_id::text IN ("+placeholders(appIds.size())+")";args.addAll(appIds);}
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
        var actorOptions=db.queryForList("SELECT id::text AS value,name AS label FROM service_a.users WHERE client_id=? ORDER BY label,value",client);
        var appOptions=db.queryForList("SELECT id::text AS value,name AS label FROM service_a.applications WHERE client_id=? ORDER BY label,value",client);
        return Map.of("items",rows,"page",page,"size",size,"totalElements",count,"totalPages",(count+size-1)/size,"actors",actorOptions,"scopes",appOptions);
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
    private org.springframework.web.server.ResponseStatusException badFilter() {return new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Invalid audit filter");}
}
