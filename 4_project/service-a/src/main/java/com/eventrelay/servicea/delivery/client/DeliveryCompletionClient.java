package com.eventrelay.servicea.delivery.client;

import com.eventrelay.servicea.auth.InternalServiceToken;
import com.eventrelay.servicea.delivery.messaging.DeliveryStatusMessage;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DeliveryCompletionClient {
    private final RestClient client;
    private final InternalServiceToken token;

    public record Page(List<DeliveryStatusMessage> items) {
    }

    public DeliveryCompletionClient(InternalServiceToken token, @Value("${app.reconciliation.service-b-url:http://127.0.0.1:8082}") String url) {
        this.token = token;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        client = RestClient.builder().baseUrl(url).requestFactory(factory).build(); //service b url
    }

    // rest api call to service b (after=time&afterId=id&size=size)
    public List<DeliveryStatusMessage> fetch(Instant after, UUID after_delivery_id, int size) {
        var page = client.get()
                .uri(uri -> uri.path("/internal/v1/delivery-completions").queryParam("after", after.toString())
                        .queryParam("afterId", after_delivery_id).queryParam("size", size).build())
                .headers(headers -> headers.setBearerAuth(token.value())).retrieve().body(Page.class);
        if (page == null || page.items() == null || page.items().size() > size)
            throw new IllegalStateException("Invalid completion feed");
        return page.items();
    }
}

// GET /internal/v1/delivery-completions?after=2026-11-23T10:00:00Z&afterId=22222222-2222-4222-8222-222222222222&size=20 HTTP/1.1
// Host: 127.0.0.1:8082
// Authorization: Bearer <internal-service-token>

// {
//   "items": [
//     {
//       "schemaVersion": 2,
//       "messageId": "66666666-6666-4666-8666-666666666666",
//       "deliveryId": "77777777-7777-4777-8777-777777777777",
//       "eventId": "33333333-3333-4333-8333-333333333333",
//       "applicationId": "44444444-4444-4444-8444-444444444444",
//       "endpointId": "55555555-5555-4555-8555-555555555555",
//       "status": "SUCCEEDED",
//       "attemptCount": 1,
//       "sequence": 1,
//       "httpStatus": 200,
//       "error": null,
//       "nextAttemptAt": null,
//       "updatedAt": "2026-11-23T10:00:05Z",
//       "attempts": [
//         {
//           "attemptNumber": 1,
//           "httpStatus": 200,
//           "error": null,
//           "startedAt": "2026-11-23T10:00:04Z",
//           "completedAt": "2026-11-23T10:00:05Z",
//           "responseBody": "{\"received\":true}",
//           "contentType": "application/json",
//           "responseTruncated": false
//         }
//       ]
//     }
//   ]
// }