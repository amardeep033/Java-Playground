package com.eventrelay.servicea.controller;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import com.eventrelay.servicea.dto.Requests.*;
import com.eventrelay.servicea.dto.Responses;
import com.eventrelay.servicea.service.RegistrationService;

@RestController
@RequestMapping("/api/v1/users/{userId}/applications")
public class ApplicationController {
    private final RegistrationService service;
    private final com.eventrelay.servicea.service.SoftDeletionService deletion;
    public ApplicationController(RegistrationService service, com.eventrelay.servicea.service.SoftDeletionService deletion) { this.service = service; this.deletion = deletion; }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.App register(@PathVariable UUID userId, @Valid @RequestBody RegisterApplication request) {
        return service.registerApplication(userId, request);
    }

    @GetMapping("")
    public Responses.PageResult<Responses.App> list(@PathVariable UUID userId, @RequestParam(defaultValue="") String q, @RequestParam(defaultValue="") String status,@RequestParam(required=false) UUID clientId, @PageableDefault(sort = "createdAt") Pageable pageable) {
        return service.listApplications(userId, q, status, clientId,pageable);
    }
    
    @GetMapping("/{applicationId}")
    public Responses.App get(@PathVariable UUID userId, @PathVariable UUID applicationId) {
        return service.getApplication(userId, applicationId);
    }

    @DeleteMapping("/{applicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId, @PathVariable UUID applicationId) {
        deletion.deleteApplication(userId, applicationId);
    }
    @PutMapping("/{applicationId}")
    public Responses.App update(@PathVariable UUID userId, @PathVariable UUID applicationId, @Valid @RequestBody UpdateApplication request) {
        return service.updateApplication(userId, applicationId, request);
    }

}
