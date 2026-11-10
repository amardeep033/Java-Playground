package com.eventrelay.servicea.reconciliation;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationWorker {
    private static final Logger log=LoggerFactory.getLogger(ReconciliationWorker.class);
    private final ReconciliationService service;
    private final boolean enabled;
    public ReconciliationWorker(ReconciliationService service,@Value("${app.reconciliation.enabled:true}") boolean enabled) {this.service=service;this.enabled=enabled;}
    @Scheduled(initialDelayString="${app.reconciliation.initial-delay-ms:60000}",fixedDelayString="${app.reconciliation.poll-delay-ms:60000}")
    public void reconcile() {
        if(!enabled) return;
        try {for(int page=0;page<5&&service.reconcilePage();page++) { /* Bounded catch-up work per tick. */ }}
        catch(RuntimeException error) {log.warn("Final delivery reconciliation failed; checkpoint retained ({})",error.getClass().getSimpleName());}
    }
}
