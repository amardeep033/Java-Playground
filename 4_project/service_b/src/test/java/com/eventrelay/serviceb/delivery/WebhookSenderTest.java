package com.eventrelay.serviceb.delivery;

import java.io.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.eventrelay.serviceb.messaging.DeliveryCommand;
import com.eventrelay.serviceb.security.SigningSecrets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookSenderTest {
    @Mock SigningSecrets secrets; @Mock HttpClient client; @Mock HttpResponse<InputStream> response;
    WebhookSender sender;
    @BeforeEach void setup() throws Exception {
        sender=new WebhookSender(secrets);ReflectionTestUtils.setField(sender,"client",client);
        when(secrets.signature(any(),any(),any(),anyLong(),any())).thenReturn("v1,test");
        when(client.<InputStream>send(any(),any())).thenReturn(response);
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type",List.of("application/json")),(a,b)->true));
    }
    @AfterEach void close(){ sender.close(); }
    @Test void responsePreviewIsBoundedWithoutLosingTheHttpOutcome() throws Exception {
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(new ByteArrayInputStream("x".repeat(9000).getBytes(StandardCharsets.UTF_8)));
        var result=sender.send(command(2000),1);
        assertTrue(result.succeeded());assertTrue(result.responseTruncated());assertEquals(4096,result.responseBody().length());
    }
    @Test void failureIncludesTheActualResponsePreview() throws Exception {
        when(response.statusCode()).thenReturn(503);
        when(response.body()).thenReturn(new ByteArrayInputStream("{\"error\":\"try later\"}".getBytes(StandardCharsets.UTF_8)));
        var result=sender.send(command(2000),1);
        assertTrue(result.retryable());assertFalse(result.succeeded());assertEquals("{\"error\":\"try later\"}",result.responseBody());
    }
    @Test void readingResponseBodyHonorsTheEndpointTimeout() throws Exception {
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(new InputStream(){
            @Override public int read() { try { Thread.sleep(1000); } catch(InterruptedException ex){ Thread.currentThread().interrupt(); } return -1; }
        });
        long start=System.nanoTime();var result=sender.send(command(100),1);
        assertFalse(result.succeeded());assertTrue(result.retryable());assertEquals("Response read timed out",result.error());
        assertTrue((System.nanoTime()-start)/1_000_000 < 900);
    }
    private DeliveryCommand command(int timeout){ return new DeliveryCommand(1,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"order.created","https://example.com/hook","{}","encrypted",1,10,4,timeout,2,1,Instant.now()); }
}
