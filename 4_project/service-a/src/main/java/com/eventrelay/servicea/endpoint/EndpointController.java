package com.eventrelay.servicea.endpoint;

import com.eventrelay.servicea.common.dto.PageResult;
import com.eventrelay.servicea.endpoint.dto.AdminEndpointUpdate;
import com.eventrelay.servicea.endpoint.dto.IssuedEndpoint;
import com.eventrelay.servicea.endpoint.dto.RegisterEndpoint;
import com.eventrelay.servicea.endpoint.dto.UpdateEndpoint;
import com.eventrelay.servicea.endpoint.dto.WebhookEndpoint;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/endpoints")
public class EndpointController {
    //request dto: RegisterEndpoint, UpdateEndpoint, AdminEndpointUpdate
    //response dto: IssuedEndpoint, WebhookEndpoint, PageResult<WebhookEndpoint>

    private final EndpointService service;

    public EndpointController(EndpointService service) {
        this.service = service;
    }

    //------------------------------------------------

    @PostMapping("")
    public ResponseEntity<IssuedEndpoint> register(@PathVariable UUID applicationId,
            @Valid @RequestBody RegisterEndpoint request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(service.registerEndpoint(applicationId, request));
    }

    @GetMapping("")
    public PageResult<WebhookEndpoint> list(@PathVariable UUID applicationId,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String status,
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return service.listEndpoints(applicationId, q, status, pageable);
    }

    @GetMapping("/{endpointId}")
    public WebhookEndpoint get(@PathVariable UUID applicationId, @PathVariable UUID endpointId) {
        return service.getEndpoint(applicationId, endpointId);
    }

    @PutMapping("/{endpointId}")
    public WebhookEndpoint update(@PathVariable UUID applicationId, @PathVariable UUID endpointId,
            @Valid @RequestBody UpdateEndpoint request) {
        return service.updateEndpoint(applicationId, endpointId, request);
    }

    @DeleteMapping("/{endpointId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID applicationId, @PathVariable UUID endpointId) {
        service.deleteEndpoint(applicationId, endpointId);
    }

    @PutMapping("/{endpointId}/admin")
    public WebhookEndpoint admin(@PathVariable UUID applicationId, @PathVariable UUID endpointId,
            @Valid @RequestBody AdminEndpointUpdate request) {
        return service.adminEndpointUpdate(applicationId, endpointId, request);
    }

    @PostMapping("/{endpointId}/signing-secret/rotate")
    public ResponseEntity<IssuedEndpoint> rotateSigningSecret(
            @PathVariable UUID applicationId, @PathVariable UUID endpointId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(service.rotateSigningSecret(applicationId, endpointId));
    }

}
