package com.eventrelay.servicea.workspace;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.eventrelay.servicea.auth.JwtUser;

/** Every dashboard operation first checks the live account and its client. No platform bypass. */
@Service
public class WorkspaceAccess {
    private final JdbcTemplate db;
    public WorkspaceAccess(JdbcTemplate db) { this.db=db; }
    public UUID clientId() {
        var actor=JwtUser.current();if(actor==null) { deny();return null; }
        var ids=db.queryForList("SELECT u.client_id FROM service_a.users u JOIN service_a.clients c ON c.id=u.client_id WHERE u.id=? AND u.deleted_at IS NULL AND c.deleted_at IS NULL",UUID.class,actor.id());
        if(ids.isEmpty()) { deny();return null; }return ids.getFirst();
    }
    public String clientRole(UUID client) {
        if(!clientId().equals(client)) { deny();return null; }
        return db.queryForObject("SELECT role FROM service_a.users WHERE id=? AND deleted_at IS NULL",String.class,JwtUser.current().id());
    }
    public void clientAdmin(UUID client) { if(!"ADMIN".equals(clientRole(client))) deny(); }
    public void lockAccount(UUID user) {
        UUID client=clientId();
        db.queryForList("SELECT id FROM service_a.clients WHERE id=? AND deleted_at IS NULL FOR SHARE",UUID.class,client);
        account(user,false);
    }
    public void account(UUID user,boolean adminRequired) {
        UUID client=clientId();
        if(!Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM service_a.users WHERE id=? AND client_id=? AND deleted_at IS NULL)",Boolean.class,user,client))) deny();
        if(adminRequired||!JwtUser.current().id().equals(user)) clientAdmin(client);
    }
    public String role(UUID app) {
        UUID client=clientId();
        if(!Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM service_a.applications WHERE id=? AND client_id=? AND deleted_at IS NULL)",Boolean.class,app,client))) deny();
        String role=clientRole(client);
        if(role.equals("USER")&&!Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM service_a.workspace_members WHERE application_id=? AND user_id=? AND client_id=? AND deleted_at IS NULL)",Boolean.class,app,JwtUser.current().id(),client))) deny();
        return role;
    }
    public void lockClientOfApp(UUID app) {
        db.queryForList("SELECT c.id FROM service_a.clients c JOIN service_a.applications a ON a.client_id=c.id WHERE a.id=? AND c.deleted_at IS NULL FOR SHARE OF c",UUID.class,app);
    }
    public void member(UUID app) { role(app); }
    public void admin(UUID app) { if(!"ADMIN".equals(role(app))) deny(); }
    public void self(UUID id) { if(JwtUser.current()==null||!JwtUser.current().id().equals(id)) deny(); }
    public static void deny() { throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You do not have permission for this resource"); }
    public static void validatePage(String q,int page,int size) {
        if(q.length()>200||page<0||size<1||size>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid filters or pagination (size 1–100)");
    }
}
