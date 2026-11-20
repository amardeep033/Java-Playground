package com.eventrelay.servicea.ingress;

import com.eventrelay.servicea.ingress.dto.IncomingRequestEntry;
import com.eventrelay.servicea.common.dto.PageResult;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
public class IncomingRequestController {
    //request dto: none (query/path parameters only)
    //response dto: PageResult<IncomingRequestEntry>

    private final IncomingRequestService service;
    public IncomingRequestController(IncomingRequestService service) { this.service=service; }
    @GetMapping("/api/v1/applications/{applicationId}/incoming-requests")
    public PageResult<IncomingRequestEntry> application(@PathVariable UUID applicationId,@RequestParam(defaultValue="") String status,
            @RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String apiKeyName,@RequestParam(defaultValue="") String authMethod,@RequestParam(required=false) Integer httpStatus,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return service.list(applicationId,status,q,apiKeyName,authMethod,httpStatus,page,size);
    }
}
