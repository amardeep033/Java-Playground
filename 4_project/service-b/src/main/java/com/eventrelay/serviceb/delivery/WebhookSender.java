package com.eventrelay.serviceb.delivery;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.*;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import com.eventrelay.serviceb.messaging.DeliveryCommand;
import com.eventrelay.serviceb.security.SigningSecrets;

@Component
public class WebhookSender {
    private final ExecutorService previews = Executors.newVirtualThreadPerTaskExecutor();
    private final SigningSecrets secrets;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    public WebhookSender(SigningSecrets secrets) { this.secrets = secrets; }
    public record Result(Integer httpStatus, String error, boolean retryable, Instant startedAt, Instant completedAt,
                         String responseBody, String contentType, boolean responseTruncated) {
        public Result(Integer status, String error, boolean retryable, Instant started, Instant completed) {
            this(status, error, retryable, started, completed, null, null, false);
        }
        public boolean succeeded() { return error == null && httpStatus != null && httpStatus >= 200 && httpStatus < 300; }
    }
    @PreDestroy public void close() { previews.close(); }
    public Result send(DeliveryCommand command, int attempt) throws InterruptedException {
        Instant started = Instant.now();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(command.timeoutMs());
        long timestamp = started.getEpochSecond();
        String signature;
        try { signature = secrets.signature(command.signingSecretEncrypted(), command.endpointId(), command.deliveryId(), timestamp, command.body()); }
        catch (IllegalStateException ex) { return new Result(null, "Signing secret unavailable", false, started, Instant.now()); }
        try {
            var request = HttpRequest.newBuilder(URI.create(command.url())).timeout(Duration.ofMillis(command.timeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "EventRelay/0.1")
                    .header("webhook-id", command.deliveryId().toString())
                    .header("webhook-timestamp", Long.toString(timestamp))
                    .header("webhook-signature", signature)
                    .header("Idempotency-Key", command.deliveryId().toString())
                    .header("X-Event-Relay-Event-Id", command.eventId().toString())
                    .header("X-Event-Relay-Event-Type", command.eventType())
                    .header("X-Event-Relay-Attempt", Integer.toString(attempt))
                    .POST(HttpRequest.BodyPublishers.ofString(command.body(), java.nio.charset.StandardCharsets.UTF_8)).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var stream = response.body()) {
                int code = response.statusCode();
                String contentType = response.headers().firstValue("Content-Type").orElse("");
                contentType = contentType.substring(0, Math.min(256, contentType.length()));
                String preview = null;
                boolean truncated = false;
                if (contentType.isEmpty() || contentType.startsWith("text/") || contentType.contains("json") || contentType.contains("xml")) {
                    Future<byte[]> reading = previews.submit(() -> stream.readNBytes(4097));
                    try {
                        byte[] bytes = reading.get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                        truncated = bytes.length > 4096;
                        preview = new String(Arrays.copyOf(bytes, Math.min(4096, bytes.length)), StandardCharsets.UTF_8);
                    } catch (TimeoutException ex) {
                        reading.cancel(true);
                        return new Result(code, "Response read timed out", true, started, Instant.now(), null, contentType, false);
                    } catch (ExecutionException ex) {
                        return new Result(code, "Response read failed", true, started, Instant.now(), null, contentType, false);
                    } finally { if (!reading.isDone()) reading.cancel(true); }
                }
                return new Result(code, code >= 200 && code < 300 ? null : "HTTP " + code, retryable(code), started, Instant.now(), preview, contentType, truncated);
            }
        } catch (IOException ex) { return new Result(null, ex.getClass().getSimpleName(), true, started, Instant.now()); }
        catch (IllegalArgumentException ex) { return new Result(null, "Invalid destination request", false, started, Instant.now()); }
    }
    public static boolean retryable(int status) { return status == 408 || status == 425 || status == 429 || status >= 500; }
}
