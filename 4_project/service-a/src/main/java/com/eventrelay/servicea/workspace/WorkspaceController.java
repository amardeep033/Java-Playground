package com.eventrelay.servicea.workspace;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class WorkspaceController {
    private final WorkspaceService workspaces;
    public WorkspaceController(WorkspaceService workspaces) { this.workspaces=workspaces; }
    public record Member(@NotNull UUID userId) {
        @com.fasterxml.jackson.annotation.JsonAnySetter public void reject(String name,Object value) { throw new IllegalArgumentException("Only userId is accepted; roles live in users.role"); }
    }
    @PutMapping("/api/v1/applications/{applicationId}/members") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void member(@PathVariable UUID applicationId,@Valid @RequestBody Member request) { workspaces.setMember(applicationId,request.userId()); }
    @DeleteMapping("/api/v1/applications/{applicationId}/members/{memberId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID applicationId,@PathVariable UUID memberId) { workspaces.removeMember(applicationId,memberId); }
}
