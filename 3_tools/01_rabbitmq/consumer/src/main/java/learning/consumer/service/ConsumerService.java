package learning.consumer.service;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsumerService {
    private static final Logger log = LoggerFactory.getLogger(ConsumerService.class);
    private final Set<String> processed = new HashSet<>();

    // Small atomic demo; production uses a unique event ID and business update in one DB transaction.
    public synchronized boolean processOnce(String id, String message) {
        
        // Idempotency check.
        if (processed.contains(id)) {
            return false;
        }
        log.info("PROCESS: id={}, message={}, thread={}", id, message, Thread.currentThread().getName());

        // Permanent failure: caught by the listener and rejected to the configured DLQ without Spring retries.
        if ("hello3".equals(message)) {
            throw new IllegalArgumentException("Permanent failure");
        }

        // Temporary failure: propagates to Spring retry, then RetryRecovery chooses retry or DLQ based on the delayed-round limit.
        if ("hello2".equals(message)) {
            throw new IllegalStateException("Temporary failure");
        }

        // Runs only after successful processing, so failed messages are not marked as completed.
        processed.add(id);
        return true;
    }
}
