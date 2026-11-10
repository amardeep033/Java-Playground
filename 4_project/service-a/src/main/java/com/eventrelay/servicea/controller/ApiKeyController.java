package com.eventrelay.servicea.controller;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.eventrelay.servicea.dto.Requests.CreateApiKey;
import com.eventrelay.servicea.dto.Responses;
import com.eventrelay.servicea.service.ApiKeyService;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/api-keys")
public class ApiKeyController {
    private final ApiKeyService service;
    public ApiKeyController(ApiKeyService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Responses.IssuedApiKey> create(@PathVariable UUID applicationId,
                                                       @Valid @RequestBody CreateApiKey request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(service.create(applicationId, request.name()));
    }

    @GetMapping
    public Responses.PageResult<Responses.ApiKeyDetails> list(@PathVariable UUID applicationId,
            @RequestParam(defaultValue="") String q, @RequestParam(defaultValue="") String status, @PageableDefault(sort = "createdAt") Pageable pageable) {
        return service.list(applicationId, q, status, pageable);
    }

    @GetMapping("/{keyId}")
    public Responses.ApiKeyDetails get(@PathVariable UUID applicationId, @PathVariable UUID keyId) {
        return service.get(applicationId, keyId);
    }

    @PostMapping("/{keyId}/rotate")
    public ResponseEntity<Responses.IssuedApiKey> rotate(@PathVariable UUID applicationId, @PathVariable UUID keyId) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(service.rotate(applicationId, keyId));
    }

    @DeleteMapping("/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID applicationId, @PathVariable UUID keyId) {
        service.revoke(applicationId, keyId);
    }
    @PutMapping("/{keyId}")
    public Responses.ApiKeyDetails update(@PathVariable UUID applicationId, @PathVariable UUID keyId, @Valid @RequestBody com.eventrelay.servicea.dto.Requests.UpdateName request) {
        return service.update(applicationId, keyId, request);
    }
}
