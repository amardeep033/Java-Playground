package com.eventrelay.servicea.workspace;

import java.util.*;
import com.eventrelay.servicea.dto.Requests.*;
import com.eventrelay.servicea.dto.Responses;
import com.eventrelay.servicea.entity.UserAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class ClientService {
    private final JdbcTemplate db;
    private final WorkspaceAccess access;
    private final AuditLogs audit;
    private final com.eventrelay.servicea.repository.UserAccountRepository users;
    private final org.springframework.security.crypto.password.PasswordEncoder passwords;
    public ClientService(JdbcTemplate db,WorkspaceAccess access,AuditLogs audit,com.eventrelay.servicea.repository.UserAccountRepository users,org.springframework.security.crypto.password.PasswordEncoder passwords) {
        this.db=db;this.access=access;this.audit=audit;this.users=users;this.passwords=passwords;
    }
    public Map<String,Object> get(UUID client) {
        String role=access.clientRole(client);
        var row=db.queryForMap("SELECT id,name,created_at FROM service_a.clients WHERE id=? AND deleted_at IS NULL",client);row.put("role",role);return row;
    }
    public record ApplicationOption(UUID id,String name,boolean active) {}
    public record UserList(List<Map<String,Object>> items,int page,int size,long totalElements,int totalPages,List<ApplicationOption> applications) {}
    public UserList users(UUID client,String q,int page,int size) {return users(client,q,"",page,size);}
    public UserList users(UUID client,String q,String role,int page,int size) {
        access.clientAdmin(client);WorkspaceAccess.validatePage(q,page,size);
        String where="client_id=? AND deleted_at IS NULL AND (lower(name) LIKE ? ESCAPE '!' OR lower(email) LIKE ? ESCAPE '!' OR lower(role) LIKE ? ESCAPE '!')";
        String pattern=com.eventrelay.servicea.service.ListSearch.pattern(q);
        var selected=com.eventrelay.servicea.service.ListSearch.choices(role,Set.of("ADMIN","USER"));
        var params=new ArrayList<Object>(List.of(client,pattern,pattern,pattern));
        if(!selected.isEmpty()){where+=" AND role IN ("+String.join(",",Collections.nCopies(selected.size(),"?"))+")";params.addAll(selected);}
        long count=db.queryForObject("SELECT count(*) FROM service_a.users WHERE "+where,Long.class,params.toArray());
        params.add(size);params.add((long)page*size);
        var rows=db.queryForList("SELECT id,name,email,role,created_at FROM service_a.users WHERE "+where+" ORDER BY created_at,id LIMIT ? OFFSET ?",params.toArray());
        // Small company app catalogue is returned once, regardless of the user page/search.
        var apps=db.query("SELECT id,name,is_active FROM service_a.applications WHERE client_id=? AND deleted_at IS NULL ORDER BY lower(name),id",
                (rs,n)->new ApplicationOption(rs.getObject("id",UUID.class),rs.getString("name"),rs.getBoolean("is_active")),client);
        var mapped=new HashMap<UUID,List<UUID>>();
        if(!rows.isEmpty()) {
            var args=new ArrayList<Object>();args.add(client);rows.forEach(row->args.add(row.get("id")));
            String ids=String.join(",",Collections.nCopies(rows.size(),"?"));
            // Fetch mappings for this user page in one query; no per-user queries or other clients' apps.
            db.queryForList("SELECT m.user_id,m.application_id FROM service_a.workspace_members m JOIN service_a.applications a ON a.id=m.application_id AND a.client_id=m.client_id WHERE m.client_id=? AND m.deleted_at IS NULL AND a.deleted_at IS NULL AND m.user_id IN ("+ids+") ORDER BY m.application_id",args.toArray())
                .forEach(row->mapped.computeIfAbsent((UUID)row.get("user_id"),key->new ArrayList<>()).add((UUID)row.get("application_id")));
        }
        var all=apps.stream().map(ApplicationOption::id).toList();
        rows.forEach(row->row.put("applicationIds",row.get("role").equals("ADMIN")?all:mapped.getOrDefault((UUID)row.get("id"),List.of())));
        return new UserList(rows,page,size,count,(int)((count+size-1)/size),apps);
    }
    @Transactional
    public Responses.User createUser(UUID client,ClientUser request) {
        lock(client);access.clientAdmin(client);
        var account=new UserAccount(request.name().strip(),request.email().strip().toLowerCase(Locale.ROOT),passwords.encode(request.password()));
        account.assignClient(client);account.changeRole(request.role());users.saveAndFlush(account);
        var apps=request.applicationIds()==null?Set.<UUID>of():request.applicationIds();
        if(request.role().equals("ADMIN")&&!apps.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Admins access every application; mappings are unnecessary");
        for(UUID app:apps) {
            if(!Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM service_a.applications WHERE id=? AND client_id=? AND deleted_at IS NULL)",Boolean.class,app,client))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Application belongs to another client or is deleted");
            db.update("INSERT INTO service_a.workspace_members(id,application_id,user_id,client_id) VALUES (?,?,?,?)",UUID.randomUUID(),app,account.getId(),client);
        }
        audit.writeClient(client,"users",account.getId(),"USER_CREATED",Map.of("role",request.role(),"applicationIds",apps));
        return Responses.User.from(account);
    }
    @Transactional
    public void updateAccount(UUID client,UUID user,AccountUpdate request) {
        lock(client);access.clientAdmin(client);var old=target(client,user);
        if(request.name()!=null) db.update("UPDATE service_a.users SET name=? WHERE id=? AND client_id=?",request.name().strip(),user,client);
        if(request.email()!=null&&!request.email().strip().equalsIgnoreCase(old.get("email").toString())) {
            String email=request.email().strip().toLowerCase(Locale.ROOT);
            audit.writeClient(client,"users",user,"EMAIL_CHANGED",Map.of("old",old.get("email"),"new",email));
            db.update("UPDATE service_a.users SET email=? WHERE id=? AND client_id=?",email,user,client);
        }
        if(request.role()!=null&&!request.role().equals(old.get("role"))) {
            if(old.get("role").equals("ADMIN")) requireAnotherAdmin(client,user);
            db.update("UPDATE service_a.users SET role=? WHERE id=? AND client_id=?",request.role(),user,client);
            // Clear old mappings on role changes; demotion never restores earlier access silently.
            db.update("UPDATE service_a.workspace_members SET deleted_at=now() WHERE user_id=? AND deleted_at IS NULL",user);
            audit.writeClient(client,"users",user,"ROLE_CHANGED",Map.of("old",old.get("role"),"new",request.role()));
        }
    }
    @Transactional
    public void resetPassword(UUID client,UUID user,String password) {
        lock(client);access.clientAdmin(client);target(client,user);
        db.update("UPDATE service_a.users SET password_hash=? WHERE id=? AND client_id=?",passwords.encode(password),user,client);
        db.update("UPDATE service_a.refresh_token_families SET revoked_at=now(),revocation_reason='PASSWORD_CHANGED' WHERE user_id=? AND revoked_at IS NULL",user);
        audit.writeClient(client,"users",user,"PASSWORD_CHANGED",Map.of());
    }
    @Transactional
    public void removeUser(UUID client,UUID user) {
        lock(client);access.clientAdmin(client);var old=target(client,user);
        if(old.get("role").equals("ADMIN")) requireAnotherAdmin(client,user);
        db.update("UPDATE service_a.users SET deleted_at=now() WHERE id=? AND client_id=?",user,client);
        db.update("UPDATE service_a.refresh_token_families SET revoked_at=now(),revocation_reason='ACCOUNT_DELETED' WHERE user_id=? AND revoked_at IS NULL",user);
        db.update("UPDATE service_a.workspace_members SET deleted_at=now() WHERE user_id=? AND deleted_at IS NULL",user);
        audit.writeClient(client,"users",user,"DELETED",Map.of());
    }
    private Map<String,Object> target(UUID client,UUID user) {
        return db.queryForList("SELECT email,role FROM service_a.users WHERE id=? AND client_id=? AND deleted_at IS NULL",user,client).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Client user not found"));
    }
    private void lock(UUID client) { db.queryForList("SELECT id FROM service_a.clients WHERE id=? AND deleted_at IS NULL FOR UPDATE",UUID.class,client); }
    private void requireAnotherAdmin(UUID client,UUID user) {
        if(!Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM service_a.users WHERE client_id=? AND id<>? AND role='ADMIN' AND deleted_at IS NULL)",Boolean.class,client,user))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Keep at least one company ADMIN");
    }
}
