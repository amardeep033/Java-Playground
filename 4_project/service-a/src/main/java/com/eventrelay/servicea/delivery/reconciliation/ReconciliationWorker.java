package com.eventrelay.servicea.delivery.reconciliation;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationWorker {
    private static final Logger log = LoggerFactory.getLogger(ReconciliationWorker.class); // catch failures and handle them without propagating the exception
    private final ReconciliationService service;
    private final boolean enabled;

    public ReconciliationWorker(ReconciliationService service, @Value("${app.reconciliation.enabled:true}") boolean enabled) {
        this.service = service;
        this.enabled = enabled;
    }

    @Scheduled(initialDelayString = "${app.reconciliation.initial-delay-ms:60000}", fixedDelayString = "${app.reconciliation.poll-delay-ms:60000}")
    public void reconcile() {
        if (!enabled)
            return;

        try {
            // Allow at most five page-processing calls
            // Increment after a successful true condition.
            for (int page = 0; page < 5 && service.reconcilePage(); page++) {
                // reconcilePage() returns true when it received a full page, meaning more results might remain. A partial/empty page returns false—but that page has already been processed before returning.
                // All processing already happened in the condition
            }
        } catch (RuntimeException error) {
            log.warn("Final delivery reconciliation failed; checkpoint retained ({})", error.getClass().getSimpleName());
        }
    }
}
