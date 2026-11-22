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
    public ResponseEntity<IssuedApiKey> create(@PathVariable("applicationId") UUID app_id,
            @Valid @RequestBody CreateApiKey request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(service.create(app_id, request.name()));
    }

    @GetMapping
    public PageResult<ApiKeyDetails> list(@PathVariable("applicationId") UUID app_id,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String status,
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return service.list(app_id, q, status, pageable);
    }

    @GetMapping("/{keyId}")
    public ApiKeyDetails get(@PathVariable("applicationId") UUID app_id, @PathVariable("keyId") UUID api_key_id) {
        return service.get(app_id, api_key_id);
    }

    @PutMapping("/{keyId}")
    public ApiKeyDetails update(@PathVariable("applicationId") UUID app_id, @PathVariable("keyId") UUID api_key_id,
            @Valid @RequestBody UpdateName request) {
        return service.update(app_id, api_key_id, request);
    }

    @DeleteMapping("/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable("applicationId") UUID app_id, @PathVariable("keyId") UUID api_key_id) {
        service.revoke(app_id, api_key_id);
    }

    @PostMapping("/{keyId}/rotate")
    public ResponseEntity<IssuedApiKey> rotate(@PathVariable("applicationId") UUID app_id, @PathVariable("keyId") UUID api_key_id) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(service.rotate(app_id, api_key_id));
    }

}
