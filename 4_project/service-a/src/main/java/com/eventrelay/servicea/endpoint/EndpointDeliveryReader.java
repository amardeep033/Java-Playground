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

    public List<EndpointDeliveryConfig> findSubscribed(UUID app_id, UUID event_type_id) {
        return endpoints.findSubscribed(app_id, event_type_id).stream().map(EndpointDeliveryConfig::from).toList();
    }

    public List<UUID> findInactiveSubscribed(UUID app_id, UUID event_type_id) {
        return endpoints.findInactiveSubscribed(app_id, event_type_id);
    }

    public Optional<EndpointDeliveryConfig> findByIdAndApplicationId(UUID endpoint_id, UUID app_id) {
        return endpoints.findByIdAndApplicationId(endpoint_id, app_id).map(EndpointDeliveryConfig::from);
    }
}
