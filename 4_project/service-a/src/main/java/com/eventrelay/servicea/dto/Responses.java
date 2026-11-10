package com.eventrelay.servicea.dto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import com.eventrelay.servicea.entity.*;
public final class Responses {
    private Responses() {}
    public record ApiKeyDetails(UUID id, UUID applicationId, String name, String prefix,
                                Instant createdAt, Instant revokedAt, UUID replacedById) {
        public static ApiKeyDetails from(ApiKey value) {
            return new ApiKeyDetails(value.getId(), value.getApplicationId(), value.getName(), value.getKeyPrefix(),
                    value.getCreatedAt(), value.getRevokedAt(), value.getReplacedById());
        }
    }
    public record IssuedApiKey(ApiKeyDetails key, String secret) {
        @Override public String toString() { return "IssuedApiKey[secret redacted]"; }
    }
    public record User(UUID id, String name, String email, Instant createdAt, String role, UUID clientId) {
        public static User from(UserAccount value) {
            return new User(value.getId(), value.getName(), value.getEmail(), value.getCreatedAt(), value.getRole(), value.getClientId());
        }
    }
    
    public record App(UUID id, UUID userId, String name, String description, Instant createdAt, boolean active,String clientRole,UUID clientId) {
        public static App from(Application value) {
            return new App(value.getId(), value.getUserId(), value.getName(), value.getDescription(), value.getCreatedAt(), value.isActive(),null,value.getClientId());
        }
        public App withRole(String role) { return new App(id,userId,name,description,createdAt,active,role,clientId); }
    }

    public record Event(UUID id, UUID applicationId, String name, String description, Instant createdAt, boolean active) {
        public static Event from(EventType value) {
            return new Event(value.getId(), value.getApplicationId(), value.getName(), value.getDescription(), value.getCreatedAt(), value.isActive());
        }
    }

    public record WebhookEndpoint(UUID id, UUID applicationId, String url, String description,
                                  List<UUID> eventTypeIds, Instant createdAt, Requests.EndpointConfiguration configuration,
                                  int signingSecretVersion, Instant signingSecretRotatedAt, boolean active,String name) {
        public static WebhookEndpoint from(Endpoint value) {
            return new WebhookEndpoint(value.getId(), value.getApplicationId(), value.getUrl(), value.getDescription(),
                    value.getEventTypes().stream().map(EventType::getId).sorted().toList(), value.getCreatedAt(),
                    new Requests.EndpointConfiguration(value.isEnabled(), value.getRateLimitPerSecond(), value.getConcurrencyLimit(), value.getTimeoutMs(), value.getMaxAttempts()),
                    value.getSigningSecretVersion(), value.getSigningSecretRotatedAt(), value.isEnabled(),value.getName());
        }
    }
    
    public record IssuedEndpoint(WebhookEndpoint endpoint, String signingSecret) {
        @Override public String toString() { return "IssuedEndpoint[signing secret redacted]"; }
    }

    public record PageResult<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResult<T> from(Page<T> page) {
            return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
}
