package com.example.asyncdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AsyncNotificationService.class);

    // Return type is void because this is fire-and-forget demo work.
    // @Async works through a Spring proxy, so the call must come from another Spring bean.
    // If this method opened a transaction, it would be a separate transaction on the notification thread.
    // For important external side effects, prefer a durable outbox instead of only fire-and-forget async.

    @Async("notificationExecutor")
    public void sendCompletionNotification(String jobId) {
        log.info("Sending completion notification for job {} on thread {}", jobId, Thread.currentThread().getName());
    }
}
