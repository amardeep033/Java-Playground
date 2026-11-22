package com.eventrelay.servicea.workspace;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class WorkspaceController {
    //request dto: Member
    //response dto: none (no response body)

    private final WorkspaceService workspaces;
    public WorkspaceController(WorkspaceService workspaces) { this.workspaces=workspaces; }
    public record Member(@NotNull UUID userId) {
        @JsonAnySetter public void reject(String name,Object value) { throw new IllegalArgumentException("Only userId is accepted; roles live in users.role"); }
    }
    @PutMapping("/api/v1/applications/{applicationId}/members") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void member(@PathVariable("applicationId") UUID app_id,@Valid @RequestBody Member request) { workspaces.setMember(app_id,request.userId()); }
    @DeleteMapping("/api/v1/applications/{applicationId}/members/{memberId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable("applicationId") UUID app_id,@PathVariable("memberId") UUID member_id) { workspaces.removeMember(app_id,member_id); }
}
