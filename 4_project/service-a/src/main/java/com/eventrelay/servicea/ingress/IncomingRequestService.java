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

    public PageResult<IncomingRequestEntry> list(UUID app_id, String status, String q, String api_key_name,
            String auth_method, Integer http_status, int page, int size) {
        
        // checks whether the application exists and the current user can access it
        access.application(app_id); 
        return store.list(app_id, status, q, api_key_name, auth_method, http_status, page, size);
    }
}
