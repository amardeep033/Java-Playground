package com.eventrelay.servicea.apikey;

import com.eventrelay.servicea.apikey.dto.ApiKeyDetails;
import com.eventrelay.servicea.apikey.dto.CreateApiKey;
import com.eventrelay.servicea.apikey.dto.IssuedApiKey;
import com.eventrelay.servicea.common.dto.PageResult;
import com.eventrelay.servicea.common.dto.UpdateName;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/api-keys")
public class ApiKeyController {
    
    //request dto: CreateApiKey, UpdateName
    //response dto: IssuedApiKey, ApiKeyDetails, PageResult<ApiKeyDetails>

    private final ApiKeyService service;

    public ApiKeyController(ApiKeyService service) {
        this.service = service;
    }

    //------------------------------------------------

    @PostMapping
    public ResponseEntity<IssuedApiKey> create(@PathVariable UUID applicationId,
            @Valid @RequestBody CreateApiKey request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(service.create(applicationId, request.name()));
    }

    @GetMapping
    public PageResult<ApiKeyDetails> list(@PathVariable UUID applicationId,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String status,
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return service.list(applicationId, q, status, pageable);
    }

    @GetMapping("/{keyId}")
    public ApiKeyDetails get(@PathVariable UUID applicationId, @PathVariable UUID keyId) {
        return service.get(applicationId, keyId);
    }

    @PutMapping("/{keyId}")
    public ApiKeyDetails update(@PathVariable UUID applicationId, @PathVariable UUID keyId,
            @Valid @RequestBody UpdateName request) {
        return service.update(applicationId, keyId, request);
    }

    @DeleteMapping("/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID applicationId, @PathVariable UUID keyId) {
        service.revoke(applicationId, keyId);
    }

    @PostMapping("/{keyId}/rotate")
    public ResponseEntity<IssuedApiKey> rotate(@PathVariable UUID applicationId, @PathVariable UUID keyId) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(service.rotate(applicationId, keyId));
    }

}
