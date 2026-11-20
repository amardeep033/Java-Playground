package com.eventrelay.servicea.application;

import com.eventrelay.servicea.application.dto.ApplicationResponse;
import com.eventrelay.servicea.application.dto.RegisterApplication;
import com.eventrelay.servicea.application.dto.UpdateApplication;
import com.eventrelay.servicea.common.dto.PageResult;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userId}/applications")
public class ApplicationController {

    // request dto: RegisterApplication, UpdateApplication
    // response dto: ApplicationResponse, PageResult<ApplicationResponse>

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    // ------------------------------------------------

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse register(@PathVariable UUID userId, @Valid @RequestBody RegisterApplication request) {
        return service.registerApplication(userId, request);
    }

    @GetMapping("")
    public PageResult<ApplicationResponse> list(@PathVariable UUID userId,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String status,
            @RequestParam(required = false) UUID clientId, @PageableDefault(sort = "createdAt") Pageable pageable) {
        return service.listApplications(userId, q, status, clientId, pageable);
    }

    @GetMapping("/{applicationId}")
    public ApplicationResponse get(@PathVariable UUID userId, @PathVariable UUID applicationId) {
        return service.getApplication(userId, applicationId);
    }

    @PutMapping("/{applicationId}")
    public ApplicationResponse update(@PathVariable UUID userId, @PathVariable UUID applicationId,
            @Valid @RequestBody UpdateApplication request) {
        return service.updateApplication(userId, applicationId, request);
    }

    @DeleteMapping("/{applicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId, @PathVariable UUID applicationId) {
        service.deleteApplication(userId, applicationId);
    }

}
