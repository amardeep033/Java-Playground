package com.eventrelay.servicea.workspace;

import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.auth.access.WorkspaceAccess;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    public void setMember(UUID app_id,UUID user_id) {
        var application=resources.lockedApplication(app_id);permissions.admin(app_id);resources.user(user_id);
        requireClientUser(application.getClientId(),user_id);
        int changed=db.update("INSERT INTO service_a.workspace_members(id,application_id,user_id,client_id) VALUES (?,?,?,?) ON CONFLICT(application_id,user_id) DO UPDATE SET deleted_at=NULL WHERE workspace_members.deleted_at IS NOT NULL",UUID.randomUUID(),app_id,user_id,application.getClientId());
        if(changed>0) audit.changed(app_id,"workspace_members",user_id,"mapped",false,true);
    }
    @Transactional
    public void removeMember(UUID app_id,UUID user_id) {
        var application=resources.lockedApplication(app_id);permissions.admin(app_id);
        requireClientUser(application.getClientId(),user_id);
        int changed=db.update("UPDATE service_a.workspace_members SET deleted_at=now() WHERE application_id=? AND user_id=? AND deleted_at IS NULL",app_id,user_id);
        if(changed>0) audit.changed(app_id,"workspace_members",user_id,"mapped",true,false);
    }
    private void requireClientUser(UUID client_id,UUID user_id) {
        var roles=db.queryForList("SELECT role FROM service_a.users WHERE client_id=? AND id=? AND deleted_at IS NULL",String.class,client_id,user_id);
        if(roles.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"User must belong to the application's client");
        if(!roles.getFirst().equals("USER")) throw new ResponseStatusException(HttpStatus.CONFLICT,"Client ADMIN already has every application; no mapping is needed");
    }
}
