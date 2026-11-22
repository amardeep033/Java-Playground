package com.eventrelay.servicea.ingress;

import com.eventrelay.servicea.ingress.dto.IncomingRequestEntry;
import com.eventrelay.servicea.common.dto.PageResult;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
public class IncomingRequestController {
    // request dto: none (query/path parameters only)
    // response dto: PageResult<IncomingRequestEntry>

    private final IncomingRequestService service;

    public IncomingRequestController(IncomingRequestService service) {
        this.service = service;
    }

    //-------------------------------------------

    // this is for showing the list
    // insertion in db happens via: events -> IncomingRequestFilter -> Security checks -> EventController -> EventIngestionService -> IncomingRequestFilter
    @GetMapping("/api/v1/applications/{applicationId}/incoming-requests")
    public PageResult<IncomingRequestEntry> application(@PathVariable("applicationId") UUID app_id,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String q, @RequestParam(name = "apiKeyName", defaultValue = "") String api_key_name,
            @RequestParam(name = "authMethod", defaultValue = "") String auth_method, @RequestParam(name = "httpStatus", required = false) Integer http_status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(app_id, status, q, api_key_name, auth_method, http_status, page, size);
    }
}
