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
    public ApplicationResponse register(@PathVariable("userId") UUID user_id, @Valid @RequestBody RegisterApplication request) {
        return service.registerApplication(user_id, request);
    }

    @GetMapping("")
    public PageResult<ApplicationResponse> list(@PathVariable("userId") UUID user_id,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String status,
            @RequestParam(name = "clientId", required = false) UUID client_id, @PageableDefault(sort = "createdAt") Pageable pageable) {
        return service.listApplications(user_id, q, status, client_id, pageable);
    }

    @GetMapping("/{applicationId}")
    public ApplicationResponse get(@PathVariable("userId") UUID user_id, @PathVariable("applicationId") UUID app_id) {
        return service.getApplication(user_id, app_id);
    }

    @PutMapping("/{applicationId}")
    public ApplicationResponse update(@PathVariable("userId") UUID user_id, @PathVariable("applicationId") UUID app_id,
            @Valid @RequestBody UpdateApplication request) {
        return service.updateApplication(user_id, app_id, request);
    }

    @DeleteMapping("/{applicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("userId") UUID user_id, @PathVariable("applicationId") UUID app_id) {
        service.deleteApplication(user_id, app_id);
    }

}
