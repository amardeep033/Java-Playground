package com.eventrelay.servicea.ingress;

import java.util.*;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.eventrelay.servicea.ingress.IncomingRequestContext.*;

class IncomingRequestFilterTest {
    private final IncomingRequestStore store=mock(IncomingRequestStore.class);
    private final IncomingRequestFilter filter=new IncomingRequestFilter(store);
    private MockHttpServletRequest request() {return new MockHttpServletRequest("POST","/api/v1/applications/"+UUID.randomUUID()+"/events");}
    private IncomingRequestStore.Entry entry() {
        var capture=ArgumentCaptor.forClass(IncomingRequestStore.Entry.class);verify(store).append(capture.capture());return capture.getValue();
    }
    @Test void capturesSecurityRejectionWithoutReadingOrPersistingCredentials() throws Exception {
        var request=request();request.addHeader("X-API-Key","private-api-key");request.setContent("private-body".getBytes());
        var response=new MockHttpServletResponse();
        filter.doFilter(request,response,(req,res)->((jakarta.servlet.http.HttpServletResponse)res).setStatus(401));
        var row=entry();assertEquals("REJECTED",row.outcome());assertEquals("UNAUTHORIZED",row.reasonCode());
        assertEquals("API_KEY",row.authMethod());assertNull(row.eventId());assertFalse(row.idempotencyKeyPresent());
        assertFalse(row.toString().contains("private-api-key"));assertFalse(row.toString().contains("private-body"));
        assertEquals(row.id().toString(),response.getHeader("X-Request-Id"));
    }
    @Test void failedChainCreatesErrorAuditWithoutInventingAnEvent() {
        var request=request();request.setAttribute(EVENT,UUID.randomUUID());
        assertThrows(ServletException.class,()->filter.doFilter(request,new MockHttpServletResponse(),(req,res)->{throw new ServletException("private-error");}));
        var row=entry();assertEquals(500,row.httpStatus());assertEquals("ERROR",row.outcome());assertNull(row.eventId());assertFalse(row.reason().contains("private-error"));
    }
    @Test void acceptedRequestRetainsEventAndSkippedEndpointMetadata() throws Exception {
        var request=request();UUID event=UUID.randomUUID(),endpoint=UUID.randomUUID();
        request.setAttribute(EVENT,event);request.setAttribute(SKIPPED,List.of(endpoint));request.setAttribute(REASON,Reason.INACTIVE_ENDPOINTS_SKIPPED);
        filter.doFilter(request,new MockHttpServletResponse(),(req,res)->((jakarta.servlet.http.HttpServletResponse)res).setStatus(202));
        var row=entry();assertEquals(event,row.eventId());assertEquals(List.of(endpoint),row.skippedEndpoints());assertEquals("ACCEPTED",row.outcome());
    }
    @Test void auditFailureDoesNotChangeAlreadyAcceptedResponse() throws Exception {
        doThrow(new IllegalStateException("database unavailable")).when(store).append(any());var response=new MockHttpServletResponse();
        filter.doFilter(request(),response,(req,res)->((jakarta.servlet.http.HttpServletResponse)res).setStatus(202));assertEquals(202,response.getStatus());
    }
    @Test void monitoringReadsAreNotEventSubmissions() throws Exception {
        var request=request();request.setMethod("GET");filter.doFilter(request,new MockHttpServletResponse(),new MockFilterChain());verifyNoInteractions(store);
    }
    @Test void cannotIngestWithoutFirstPersistingTheParentRequest() throws Exception {
        doThrow(new IllegalStateException("database unavailable")).when(store).begin(any(),any());
        var response=new MockHttpServletResponse();var chain=new MockFilterChain();
        filter.doFilter(request(),response,chain);
        assertEquals(503,response.getStatus());assertNull(chain.getRequest());verify(store,never()).append(any());
    }
}
