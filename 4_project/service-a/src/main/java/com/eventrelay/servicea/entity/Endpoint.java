package com.eventrelay.servicea.entity;
import java.util.*;
import jakarta.persistence.*;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@org.hibernate.annotations.SQLDelete(sql = "UPDATE service_a.endpoints SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Entity
@Table(name = "endpoints")
public class Endpoint extends BaseEntity {
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name="name",nullable=false,length=120)
    private String name="Endpoint";
    public String getName() { return name; }
    public void updateName(String name) { this.name=name; }

    @Column(name = "url", nullable = false, length = 2048)
    private String url;
    public void updateUrl(String url) { this.url=url; }
    public void updateSubscriptions(Set<EventType> types) { this.eventTypes=new HashSet<>(types); }

    @Column(name = "description", nullable = true, length = 500)
    private String description;

    @Column(name="is_active", nullable = false)
    private boolean enabled = true;
    @Column(name = "rate_limit_per_second", nullable = false)
    private int rateLimitPerSecond = 10;
    @Column(name = "concurrency_limit", nullable = false)
    private int concurrencyLimit = 4;
    @Column(name = "timeout_ms", nullable = false)
    private int timeoutMs = 10000;
    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;
    @Column(name = "signing_secret_encrypted", columnDefinition = "text")
    private String signingSecretEncrypted;
    @Column(name = "signing_secret_version", nullable = false)
    private int signingSecretVersion;
    @Column(name = "signing_secret_rotated_at")
    private java.time.Instant signingSecretRotatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "endpoint_event_types",
            joinColumns = @JoinColumn(name = "endpoint_id"),
            inverseJoinColumns = @JoinColumn(name = "event_type_id"))
    private Set<EventType> eventTypes = new HashSet<>();
    
    protected Endpoint() {}
    public Endpoint(UUID applicationId, String url, String description, Set<EventType> eventTypes) {
        this.applicationId = applicationId;
        this.url = url;
        this.description = description;
        this.eventTypes = new HashSet<>(eventTypes);
    }
    public UUID getApplicationId() { return applicationId; }
    public String getUrl() { return url; }
    public String getDescription() { return description; }
    public Set<EventType> getEventTypes() { return Collections.unmodifiableSet(eventTypes); }

    public void setActive(boolean active) { this.enabled=active; }
    public boolean isEnabled() { return enabled; }
    public int getRateLimitPerSecond() { return rateLimitPerSecond; }
    public int getConcurrencyLimit() { return concurrencyLimit; }
    public int getTimeoutMs() { return timeoutMs; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getSigningSecretEncrypted() { return signingSecretEncrypted; }
    public int getSigningSecretVersion() { return signingSecretVersion; }
    public java.time.Instant getSigningSecretRotatedAt() { return signingSecretRotatedAt; }
    public void configure(boolean enabled, int rate, int concurrency, int timeout, int attempts) {
        this.enabled = enabled;
        this.rateLimitPerSecond = rate;
        this.concurrencyLimit = concurrency;
        this.timeoutMs = timeout;
        this.maxAttempts = attempts;
    }
    public void rotateSigningSecret(String encrypted) {
        signingSecretEncrypted = encrypted;
        signingSecretVersion++;
        signingSecretRotatedAt = java.time.Instant.now();
    }
    public void updateDescription(String description) { this.description=description; }
}
