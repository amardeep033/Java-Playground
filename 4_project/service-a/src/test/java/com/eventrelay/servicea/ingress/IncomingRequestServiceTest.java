package com.eventrelay.servicea.ingress;

import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.common.dto.PageResult;
import com.eventrelay.servicea.ingress.dto.IncomingRequestEntry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IncomingRequestServiceTest {
    private final ResourceAccess access = mock(ResourceAccess.class);
    private final IncomingRequestStore store = mock(IncomingRequestStore.class);
    private final IncomingRequestService service = new IncomingRequestService(store, access);
    private final UUID app = UUID.randomUUID();

    @Test
    void deniedMembershipDoesNotReadRequestHistory() {
        when(access.application(app)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        assertThrows(ResponseStatusException.class, () -> service.list(app, "", "", "", "", null, 0, 20));
        verifyNoInteractions(store);
    }

    @Test
    void authorizedHistoryQueryPreservesApplicationScopeAndFilters() {
        var result = new PageResult<IncomingRequestEntry>(List.of(), 2, 20, 0, 0);
        when(store.list(app, "REJECTED", "order", "Customer key", "API_KEY", 409, 2, 20)).thenReturn(result);
        assertSame(result, service.list(app, "REJECTED", "order", "Customer key", "API_KEY", 409, 2, 20));
        var order = inOrder(access, store);
        order.verify(access).application(app);
        order.verify(store).list(app, "REJECTED", "order", "Customer key", "API_KEY", 409, 2, 20);
    }
}
