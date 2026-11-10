package com.eventrelay.servicea.reconciliation;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.eventrelay.servicea.config.InternalServiceToken;
import com.eventrelay.servicea.messaging.DeliveryStatusMessage;

@Component
public class CompletionClient {
    private final RestClient client;
    private final InternalServiceToken token;
    public record Page(List<DeliveryStatusMessage> items) {}
    public CompletionClient(InternalServiceToken token,@Value("${app.reconciliation.service-b-url:http://127.0.0.1:8082}") String url) {
        this.token=token;
        var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(Duration.ofSeconds(3));factory.setReadTimeout(Duration.ofSeconds(10));
        client=RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
    public List<DeliveryStatusMessage> fetch(Instant after,UUID id,int size) {
        var page=client.get().uri(uri->uri.path("/internal/v1/delivery-completions").queryParam("after",after.toString()).queryParam("afterId",id).queryParam("size",size).build())
                .headers(headers->headers.setBearerAuth(token.value())).retrieve().body(Page.class);
        if(page==null||page.items()==null||page.items().size()>size) throw new IllegalStateException("Invalid completion feed");
        return page.items();
    }
}
