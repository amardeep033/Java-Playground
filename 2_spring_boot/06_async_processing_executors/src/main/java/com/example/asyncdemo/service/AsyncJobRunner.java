package com.example.asyncdemo.service;

import com.example.asyncdemo.model.JobRecord;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;



@Service
public class AsyncJobRunner {

    private static final Logger log = LoggerFactory.getLogger(AsyncJobRunner.class);

    private final AsyncNotificationService notificationService;

    public AsyncJobRunner(AsyncNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    //-----------------------------------------------------------------------------------

    // async call1: runs the submitted job on jobExecutor.
    @Async("jobExecutor")
    public CompletableFuture<Void> runJob(JobRecord job, int seconds) {
        log.info("Starting job {} on thread {}", job.id(), Thread.currentThread().getName());
        job.markRunning();

        try {
            Thread.sleep(seconds * 1000L); // simulates slow blocking work, like a remote call or report generation
            job.markSucceeded("Completed after " + seconds + " second(s)");

            // This is another async call, but it is not the reason for the try-catch.
            // The try-catch protects the job state from failures in this worker method.
            // If notification fails later, that failure belongs to the notification async method.
            // Note: in real code, keep non-critical notification failure from changing job success.
            // If notification submission is rejected synchronously here, this catch block can mark the job FAILED
            // even though the actual job already succeeded. Usually notification should be retried/logged separately.
            notificationService.sendCompletionNotification(job.id());
            return CompletableFuture.completedFuture(null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            job.markFailed(exception);
            return CompletableFuture.failedFuture(exception);
        } catch (RuntimeException exception) {
            job.markFailed(exception);
            return CompletableFuture.failedFuture(exception);
        }
    }

    //-----------------------------------------------------------------------------------

    // async call2: independent report input.
    @Async("jobExecutor")
    public CompletableFuture<Integer> countOrdersForReport() {
        sleep(700);
        return CompletableFuture.completedFuture(42);
    }

    // async call3: another independent report input.
    @Async("jobExecutor")
    public CompletableFuture<Integer> countPaymentsForReport() {
        sleep(900);
        return CompletableFuture.completedFuture(39);
    }

    //-----------------------------------------------------------------------------------

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Async work interrupted", exception);
        }
    }
}


// @Transactional
// public void createOrder() {
//     saveOrder();
//     asyncService.updateAnalytics(); // runs later on another thread, outside this transaction
// }
// This is dangerous when updateAnalytics() must never be lost:
// the order transaction can commit, then the app can crash before/during async analytics.
// The async method also runs on another thread, so it does not share the caller's transaction.
// For reliable event publishing, write an outbox row in the same transaction and publish it later.
