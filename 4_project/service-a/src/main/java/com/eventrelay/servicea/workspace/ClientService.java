package com.eventrelay.servicea.workspace;

import com.eventrelay.servicea.auth.access.WorkspaceAccess;
import com.eventrelay.servicea.common.search.ListSearch;
import com.eventrelay.servicea.user.UserAccount;
import com.eventrelay.servicea.user.UserAccountRepository;
import com.eventrelay.servicea.user.dto.UserResponse;
import com.eventrelay.servicea.workspace.dto.AccountUpdate;
import com.eventrelay.servicea.workspace.dto.ClientUser;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class ClientService {
    private final JdbcTemplate db;
    private final WorkspaceAccess access;
    private final AuditLogs audit;
    private final UserAccountRepository users;
    private final PasswordEncoder passwords;
    public ClientService(JdbcTemplate db,WorkspaceAccess access,AuditLogs audit,UserAccountRepository users,PasswordEncoder passwords) {
        this.db=db;this.access=access;this.audit=audit;this.users=users;this.passwords=passwords;
    }
    public Map<String,Object> get(UUID client_id) {
        String role=access.clientRole(client_id);
        var row=db.queryForMap("SELECT id,name,created_at FROM service_a.clients WHERE id=? AND deleted_at IS NULL",client_id);row.put("role",role);return row;
    }
    public record ApplicationOption(UUID id,String name,boolean active) {}
    public record UserList(List<Map<String,Object>> items,int page,int size,long totalElements,int totalPages,List<ApplicationOption> applications) {}
    public UserList users(UUID client_id,String q,int page,int size) {return users(client_id,q,"",page,size);}
    public UserList users(UUID client_id,String q,String role,int page,int size) {
        access.clientAdmin(client_id);WorkspaceAccess.validatePage(q,page,size);
        String where="client_id=? AND deleted_at IS NULL AND (lower(name) LIKE ? ESCAPE '!' OR lower(email) LIKE ? ESCAPE '!' OR lower(role) LIKE ? ESCAPE '!')";
        String pattern=ListSearch.pattern(q);
        var selected=ListSearch.choices(role,Set.of("ADMIN","USER"));
        var params=new ArrayList<Object>(List.of(client_id,pattern,pattern,pattern));
        if(!selected.isEmpty()){where+=" AND role IN ("+String.join(",",Collections.nCopies(selected.size(),"?"))+")";params.addAll(selected);}
        long count=db.queryForObject("SELECT count(*) FROM service_a.users WHERE "+where,Long.class,params.toArray());
        params.add(size);params.add((long)page*size);
        var rows=db.queryForList("SELECT id,name,email,role,created_at FROM service_a.users WHERE "+where+" ORDER BY created_at,id LIMIT ? OFFSET ?",params.toArray());
        // Small company app catalogue is returned once, regardless of the user page/search.
        var apps=db.query("SELECT id,name,is_active FROM service_a.applications WHERE client_id=? AND deleted_at IS NULL ORDER BY lower(name),id",
                (rs,n)->new ApplicationOption(rs.getObject("id",UUID.class),rs.getString("name"),rs.getBoolean("is_active")),client_id);
        var mapped=new HashMap<UUID,List<UUID>>();
        if(!rows.isEmpty()) {
            var args=new ArrayList<Object>();args.add(client_id);rows.forEach(row->args.add(row.get("id")));
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
    public UserResponse createUser(UUID client_id,ClientUser request) {
        lock(client_id);access.clientAdmin(client_id);
        var account=new UserAccount(request.name().strip(),request.email().strip().toLowerCase(Locale.ROOT),passwords.encode(request.password()));
        account.assignClient(client_id);account.changeRole(request.role());users.saveAndFlush(account);
        var apps=request.applicationIds()==null?Set.<UUID>of():request.applicationIds();
        if(request.role().equals("ADMIN")&&!apps.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Admins access every application; mappings are unnecessary");
        for(UUID app_id:apps) {
            if(!Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM service_a.applications WHERE id=? AND client_id=? AND deleted_at IS NULL)",Boolean.class,app_id,client_id))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Application belongs to another client or is deleted");
            db.update("INSERT INTO service_a.workspace_members(id,application_id,user_id,client_id) VALUES (?,?,?,?)",UUID.randomUUID(),app_id,account.getId(),client_id);
        }
        audit.writeClient(client_id,"users",account.getId(),"USER_CREATED",Map.of("role",request.role(),"applicationIds",apps));
        return UserResponse.from(account);
    }
    @Transactional
    public void updateAccount(UUID client_id,UUID user_id,AccountUpdate request) {
        lock(client_id);access.clientAdmin(client_id);var old=target(client_id,user_id);
        if(request.name()!=null) db.update("UPDATE service_a.users SET name=? WHERE id=? AND client_id=?",request.name().strip(),user_id,client_id);
        if(request.email()!=null&&!request.email().strip().equalsIgnoreCase(old.get("email").toString())) {
            String email=request.email().strip().toLowerCase(Locale.ROOT);
            audit.writeClient(client_id,"users",user_id,"EMAIL_CHANGED",Map.of("old",old.get("email"),"new",email));
            db.update("UPDATE service_a.users SET email=? WHERE id=? AND client_id=?",email,user_id,client_id);
        }
        if(request.role()!=null&&!request.role().equals(old.get("role"))) {
            if(old.get("role").equals("ADMIN")) requireAnotherAdmin(client_id,user_id);
            db.update("UPDATE service_a.users SET role=? WHERE id=? AND client_id=?",request.role(),user_id,client_id);
            // Clear old mappings on role changes; demotion never restores earlier access silently.
            db.update("UPDATE service_a.workspace_members SET deleted_at=now() WHERE user_id=? AND deleted_at IS NULL",user_id);
            audit.writeClient(client_id,"users",user_id,"ROLE_CHANGED",Map.of("old",old.get("role"),"new",request.role()));
        }
    }
    @Transactional
    public void resetPassword(UUID client_id,UUID user_id,String password) {
        lock(client_id);access.clientAdmin(client_id);target(client_id,user_id);
        db.update("UPDATE service_a.users SET password_hash=? WHERE id=? AND client_id=?",passwords.encode(password),user_id,client_id);
        db.update("UPDATE service_a.refresh_token_families SET revoked_at=now(),revocation_reason='PASSWORD_CHANGED' WHERE user_id=? AND revoked_at IS NULL",user_id);
        audit.writeClient(client_id,"users",user_id,"PASSWORD_CHANGED",Map.of());
    }
    @Transactional
    public void removeUser(UUID client_id,UUID user_id) {
        lock(client_id);access.clientAdmin(client_id);var old=target(client_id,user_id);
        if(old.get("role").equals("ADMIN")) requireAnotherAdmin(client_id,user_id);
        db.update("UPDATE service_a.users SET deleted_at=now() WHERE id=? AND client_id=?",user_id,client_id);
        db.update("UPDATE service_a.refresh_token_families SET revoked_at=now(),revocation_reason='ACCOUNT_DELETED' WHERE user_id=? AND revoked_at IS NULL",user_id);
        db.update("UPDATE service_a.workspace_members SET deleted_at=now() WHERE user_id=? AND deleted_at IS NULL",user_id);
        audit.writeClient(client_id,"users",user_id,"DELETED",Map.of());
    }
    private Map<String,Object> target(UUID client_id,UUID user_id) {
        return db.queryForList("SELECT email,role FROM service_a.users WHERE id=? AND client_id=? AND deleted_at IS NULL",user_id,client_id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Client user not found"));
    }
    private void lock(UUID client_id) { db.queryForList("SELECT id FROM service_a.clients WHERE id=? AND deleted_at IS NULL FOR UPDATE",UUID.class,client_id); }
    private void requireAnotherAdmin(UUID client_id,UUID user_id) {
        if(!Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM service_a.users WHERE client_id=? AND id<>? AND role='ADMIN' AND deleted_at IS NULL)",Boolean.class,client_id,user_id))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Keep at least one company ADMIN");
    }
}
