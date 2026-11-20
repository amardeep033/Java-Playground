package com.eventrelay.servicea.endpoint;

import java.util.UUID;

/** Internal dispatch snapshot; never returned by dashboard APIs. */
public record EndpointDeliveryConfig(UUID id, String url, String signingSecretEncrypted,
        int signingSecretVersion, int rateLimitPerSecond, int concurrencyLimit, int timeoutMs, int maxAttempts) {
    static EndpointDeliveryConfig from(Endpoint endpoint) {
        return new EndpointDeliveryConfig(endpoint.getId(), endpoint.getUrl(), endpoint.getSigningSecretEncrypted(),
                endpoint.getSigningSecretVersion(), endpoint.getRateLimitPerSecond(), endpoint.getConcurrencyLimit(),
                endpoint.getTimeoutMs(), endpoint.getMaxAttempts());
    }

    @Override
    public String toString() {
        return "EndpointDeliveryConfig[id=" + id + ", signing secret redacted]";
    }
}
