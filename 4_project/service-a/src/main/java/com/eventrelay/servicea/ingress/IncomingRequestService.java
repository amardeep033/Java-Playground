package com.eventrelay.servicea.ingress;

import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.common.dto.PageResult;
import com.eventrelay.servicea.ingress.dto.IncomingRequestEntry;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IncomingRequestService {
    private final IncomingRequestStore store;
    private final ResourceAccess access;

    public IncomingRequestService(IncomingRequestStore store, ResourceAccess access) {
        this.store = store;
        this.access = access;
    }

    public PageResult<IncomingRequestEntry> list(UUID applicationId, String status, String q, String apiKeyName,
            String authMethod, Integer httpStatus, int page, int size) {
        access.application(applicationId); // Includes server-side workspace membership validation.
        return store.list(applicationId, status, q, apiKeyName, authMethod, httpStatus, page, size);
    }
}
