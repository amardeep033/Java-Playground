package com.eventrelay.servicea.ingress;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.eventrelay.servicea.auth.JwtUser;
import com.eventrelay.servicea.service.ResourceAccess;
import com.eventrelay.servicea.dto.Responses.PageResult;

@RestController
public class IncomingRequestController {
    private final IncomingRequestStore store;
    private final ResourceAccess access;
    public IncomingRequestController(IncomingRequestStore store,ResourceAccess access) { this.store=store;this.access=access; }
    @GetMapping("/api/v1/applications/{applicationId}/incoming-requests")
    public PageResult<IncomingRequestStore.Entry> application(@PathVariable UUID applicationId,@RequestParam(defaultValue="") String status,
            @RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String apiKeyName,@RequestParam(defaultValue="") String authMethod,@RequestParam(required=false) Integer httpStatus,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        access.application(applicationId); // Includes server-side workspace membership validation.
        return store.list(applicationId,status,q,apiKeyName,authMethod,httpStatus,page,size);
    }
}
