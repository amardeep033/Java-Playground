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
    public ResponseEntity<IssuedEndpoint> register(@PathVariable("applicationId") UUID app_id,
            @Valid @RequestBody RegisterEndpoint request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(service.registerEndpoint(app_id, request));
    }

    @GetMapping("")
    public PageResult<WebhookEndpoint> list(@PathVariable("applicationId") UUID app_id,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String status,
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return service.listEndpoints(app_id, q, status, pageable);
    }

    @GetMapping("/{endpointId}")
    public WebhookEndpoint get(@PathVariable("applicationId") UUID app_id, @PathVariable("endpointId") UUID endpoint_id) {
        return service.getEndpoint(app_id, endpoint_id);
    }

    @PutMapping("/{endpointId}")
    public WebhookEndpoint update(@PathVariable("applicationId") UUID app_id, @PathVariable("endpointId") UUID endpoint_id,
            @Valid @RequestBody UpdateEndpoint request) {
        return service.updateEndpoint(app_id, endpoint_id, request);
    }

    @DeleteMapping("/{endpointId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("applicationId") UUID app_id, @PathVariable("endpointId") UUID endpoint_id) {
        service.deleteEndpoint(app_id, endpoint_id);
    }

    @PutMapping("/{endpointId}/admin")
    public WebhookEndpoint admin(@PathVariable("applicationId") UUID app_id, @PathVariable("endpointId") UUID endpoint_id,
            @Valid @RequestBody AdminEndpointUpdate request) {
        return service.adminEndpointUpdate(app_id, endpoint_id, request);
    }

    @PostMapping("/{endpointId}/signing-secret/rotate")
    public ResponseEntity<IssuedEndpoint> rotateSigningSecret(
            @PathVariable("applicationId") UUID app_id, @PathVariable("endpointId") UUID endpoint_id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(service.rotateSigningSecret(app_id, endpoint_id));
    }

}
