package com.eventrelay.servicea.endpoint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read boundary for ingestion and replay; callers own authorization and app
 * locking.
 */
@Service
@Transactional(readOnly = true)
public class EndpointDeliveryReader {
    private final EndpointRepository endpoints;

    public EndpointDeliveryReader(EndpointRepository endpoints) {
        this.endpoints = endpoints;
    }

    public List<EndpointDeliveryConfig> findSubscribed(UUID applicationId, UUID eventTypeId) {
        return endpoints.findSubscribed(applicationId, eventTypeId).stream().map(EndpointDeliveryConfig::from).toList();
    }

    public List<UUID> findInactiveSubscribed(UUID applicationId, UUID eventTypeId) {
        return endpoints.findInactiveSubscribed(applicationId, eventTypeId);
    }

    public Optional<EndpointDeliveryConfig> findByIdAndApplicationId(UUID endpointId, UUID applicationId) {
        return endpoints.findByIdAndApplicationId(endpointId, applicationId).map(EndpointDeliveryConfig::from);
    }
}
