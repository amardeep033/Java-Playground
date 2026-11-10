package com.eventrelay.servicea.messaging;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.eventrelay.servicea.events.EventStore;
@Service
public class StatusUpdateService {
    private final EventStore store;
    public StatusUpdateService(EventStore store) { this.store = store; }
    @Transactional public void apply(DeliveryStatusMessage message) { message.validate(); store.applyStatus(message); }
}
