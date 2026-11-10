package com.eventrelay.servicea.workspace;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.eventrelay.servicea.service.ResourceAccess;

@Service
@Transactional(readOnly=true)
public class WorkspaceService {
    private final JdbcTemplate db;
    private final WorkspaceAccess permissions;
    private final ResourceAccess resources;
    private final AuditLogs audit;
    public WorkspaceService(JdbcTemplate db,WorkspaceAccess permissions,ResourceAccess resources,AuditLogs audit) {
        this.db=db;this.permissions=permissions;this.resources=resources;this.audit=audit;
    }
    @Transactional
    public void setMember(UUID app,UUID user) {
        var application=resources.lockedApplication(app);permissions.admin(app);resources.user(user);
        requireClientUser(application.getClientId(),user);
        int changed=db.update("INSERT INTO service_a.workspace_members(id,application_id,user_id,client_id) VALUES (?,?,?,?) ON CONFLICT(application_id,user_id) DO UPDATE SET deleted_at=NULL WHERE workspace_members.deleted_at IS NOT NULL",UUID.randomUUID(),app,user,application.getClientId());
        if(changed>0) audit.changed(app,"workspace_members",user,"mapped",false,true);
    }
    @Transactional
    public void removeMember(UUID app,UUID user) {
        var application=resources.lockedApplication(app);permissions.admin(app);
        requireClientUser(application.getClientId(),user);
        int changed=db.update("UPDATE service_a.workspace_members SET deleted_at=now() WHERE application_id=? AND user_id=? AND deleted_at IS NULL",app,user);
        if(changed>0) audit.changed(app,"workspace_members",user,"mapped",true,false);
    }
    private void requireClientUser(UUID client,UUID user) {
        var roles=db.queryForList("SELECT role FROM service_a.users WHERE client_id=? AND id=? AND deleted_at IS NULL",String.class,client,user);
        if(roles.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"User must belong to the application's client");
        if(!roles.getFirst().equals("USER")) throw new ResponseStatusException(HttpStatus.CONFLICT,"Client ADMIN already has every application; no mapping is needed");
    }
}
