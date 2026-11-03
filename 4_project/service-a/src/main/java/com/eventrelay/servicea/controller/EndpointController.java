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
@RequestMapping("/api/v1/applications/{applicationId}/endpoints")
public class EndpointController {
    private final RegistrationService service;
    private final com.eventrelay.servicea.service.SoftDeletionService deletion;
    public EndpointController(RegistrationService service, com.eventrelay.servicea.service.SoftDeletionService deletion) { this.service = service; this.deletion = deletion; }

    @PostMapping("")
    public org.springframework.http.ResponseEntity<Responses.IssuedEndpoint> register(@PathVariable UUID applicationId, @Valid @RequestBody RegisterEndpoint request) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED).cacheControl(org.springframework.http.CacheControl.noStore()).body(service.registerEndpoint(applicationId, request));
    }

    @GetMapping("")
    public Responses.PageResult<Responses.WebhookEndpoint> list(@PathVariable UUID applicationId, @PageableDefault(sort = "createdAt") Pageable pageable) {
        return service.listEndpoints(applicationId, pageable);
    }
    
    @GetMapping("/{endpointId}")
    public Responses.WebhookEndpoint get(@PathVariable UUID applicationId, @PathVariable UUID endpointId) {
        return service.getEndpoint(applicationId, endpointId);
    }

    @DeleteMapping("/{endpointId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID applicationId, @PathVariable UUID endpointId) {
        deletion.deleteEndpoint(applicationId, endpointId);
    }

    @PutMapping("/{endpointId}/configuration")
    public Responses.WebhookEndpoint configure(@PathVariable UUID applicationId, @PathVariable UUID endpointId,
                                               @Valid @RequestBody EndpointConfiguration request) {
        return service.configureEndpoint(applicationId, endpointId, request);
    }

    @PostMapping("/{endpointId}/signing-secret/rotate")
    public org.springframework.http.ResponseEntity<Responses.IssuedEndpoint> rotateSigningSecret(
            @PathVariable UUID applicationId, @PathVariable UUID endpointId) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(org.springframework.http.CacheControl.noStore()).body(service.rotateSigningSecret(applicationId, endpointId));
    }
}
