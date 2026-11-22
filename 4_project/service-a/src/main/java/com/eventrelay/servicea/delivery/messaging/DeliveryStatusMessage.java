package com.eventrelay.servicea.delivery.messaging;

import java.time.Instant;
import java.util.*;

// {
//   "schemaVersion": 2,
//   "messageId": "66666666-6666-4666-8666-666666666666",
//   "deliveryId": "22222222-2222-4222-8222-222222222222",
//   "eventId": "33333333-3333-4333-8333-333333333333",
//   "applicationId": "44444444-4444-4444-8444-444444444444",
//   "endpointId": "55555555-5555-4555-8555-555555555555",
//   "status": "SUCCEEDED",
//   "attemptCount": 2,
//   "sequence": 2,
//   "httpStatus": 200,
//   "error": null,
//   "nextAttemptAt": null,
//   "updatedAt": "2026-11-22T10:00:32Z",
//   "attempts": [
//     {
//       "attemptNumber": 1,
//       "httpStatus": 503,
//       "error": "HTTP 503",
//       "startedAt": "2026-11-22T10:00:00Z",
//       "completedAt": "2026-11-22T10:00:01Z",
//       "responseBody": "Service unavailable",
//       "contentType": "text/plain",
//       "responseTruncated": false
//     },
//     {
//       "attemptNumber": 2,
//       "httpStatus": 200,
//       "error": null,
//       "startedAt": "2026-11-22T10:00:31Z",
//       "completedAt": "2026-11-22T10:00:32Z",
//       "responseBody": "{\"received\":true}",
//       "contentType": "application/json",
//       "responseTruncated": false
//     }
//   ]
// }

/** Final, immutable completion snapshot; retries stay inside Service B. */
public record DeliveryStatusMessage(int schemaVersion, UUID messageId, UUID deliveryId, UUID eventId,
        UUID applicationId, UUID endpointId, String status, int attemptCount, long sequence,
        Integer httpStatus, String error, Instant nextAttemptAt, Instant updatedAt, List<Attempt> attempts) {

    public void validate() {

        if (schemaVersion != 2 || messageId == null || deliveryId == null || eventId == null || applicationId == null
                || endpointId == null
                || updatedAt == null || sequence < 1 || attemptCount < 1 || attemptCount > 20 || nextAttemptAt != null
                || status == null || !Set.of("SUCCEEDED", "DEAD").contains(status)
                || !validHttp(httpStatus) || error != null && error.length() > 1000
                || attempts == null || attempts.size() != attemptCount)
            throw new IllegalArgumentException("Invalid final delivery status");
            
        for (int i = 0; i < attempts.size(); i++) {
            var a = attempts.get(i); 
            if (a == null || a.attemptNumber() != i + 1 || a.startedAt() == null || a.completedAt() == null
                    || a.completedAt().isBefore(a.startedAt()) || !validHttp(a.httpStatus())
                    || a.responseBody() != null && a.responseBody().length() > 4096
                    || a.contentType() != null && a.contentType().length() > 256
                    || a.error() != null && a.error().length() > 1000)
                throw new IllegalArgumentException("Invalid delivery attempt");
        }

        var last = attempts.getLast();
        if (!Objects.equals(httpStatus, last.httpStatus()) || !Objects.equals(error, last.error())
                || status.equals("SUCCEEDED")
                        && (error != null || httpStatus == null || httpStatus < 200 || httpStatus >= 300)
                || status.equals("DEAD") && error == null && httpStatus != null && httpStatus >= 200
                        && httpStatus < 300)
            throw new IllegalArgumentException("Completion does not match its final attempt");
    }

    // ------------------------------------------

    public record Attempt(int attemptNumber, Integer httpStatus, String error, Instant startedAt,
            Instant completedAt, String responseBody, String contentType, boolean responseTruncated) {
    }

    private static boolean validHttp(Integer value) {
        return value == null || value >= 100 && value <= 599;
    }
}
