package com.eventrelay.servicea.delivery.messaging;

import com.eventrelay.servicea.delivery.persistence.DeliveryProjectionStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusUpdateService {
    private final DeliveryProjectionStore store;
    public StatusUpdateService(DeliveryProjectionStore store) { this.store = store; }
    @Transactional public void apply(DeliveryStatusMessage message) { message.validate(); store.applyStatus(message); }
}
