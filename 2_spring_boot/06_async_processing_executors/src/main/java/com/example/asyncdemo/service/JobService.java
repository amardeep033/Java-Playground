package com.example.asyncdemo.service;

import com.example.asyncdemo.dto.JobResponse;
import com.example.asyncdemo.dto.SubmitJobRequest;
import com.example.asyncdemo.model.JobRecord;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;



@Service
public class JobService {

    private final Map<String, JobRecord> jobs = new ConcurrentHashMap<>();
    private final AsyncJobRunner asyncJobRunner;

    public JobService(AsyncJobRunner asyncJobRunner) {
        this.asyncJobRunner = asyncJobRunner;
    }

    //----------------------------------------------------------

    // No try-catch here for failures inside runJob().
    // This method only submits work and returns 202-style state to the caller.
    // Once the @Async method starts on another thread, its exception is not thrown back to this call stack.
    // runJob() catches task failures, marks the job FAILED, and returns a failed CompletableFuture.
    public JobResponse submitJob(SubmitJobRequest request) {
        String id = UUID.randomUUID().toString();
        JobRecord job = new JobRecord(id, request.description());
        jobs.put(id, job);

        // async call1: submit the job and return immediately; final result is observed later by polling.
        // Note: in real code, handle executor rejection here.
        // If the pool is saturated, @Async submission can fail before the response is returned.
        // Without handling that case, the job may stay ACCEPTED in memory but never actually run.
        asyncJobRunner.runJob(job, request.seconds());
        return job.toResponse();
    }

    //----------------------------------------------------------

    public Optional<JobResponse> findJob(String id) {
        return Optional.ofNullable(jobs.get(id)).map(JobRecord::toResponse);
    }

    //----------------------------------------------------------

    public List<JobResponse> listJobs() {
        return jobs.values().stream()
                .map(JobRecord::toResponse)
                .sorted(Comparator.comparing(JobResponse::createdAt).reversed())
                .toList();
    }

    //----------------------------------------------------------

    public CompletableFuture<String> runReport() {
        // async call2: pretend this is one independent IO/database call.
        CompletableFuture<Integer> orders = asyncJobRunner.countOrdersForReport();

        // async call3: pretend this is another independent IO/database call.
        CompletableFuture<Integer> payments = asyncJobRunner.countPaymentsForReport();

        // These represent two independent IO-style calls running in parallel.
        // The caller does not block between them; thenCombine builds the final response when both futures complete.

        return orders.thenCombine(payments, (orderCount, paymentCount) ->
                "Report combined on " + Thread.currentThread().getName()
                        + ": orders=" + orderCount
                        + ", payments=" + paymentCount);
    }
}

// Exception handling:
// 0. This will not catch async failures because the try block only surrounds task submission:
// try {
//     CompletableFuture.supplyAsync(() -> processOrder());
// } catch (Exception e) {
//     // exceptions thrown later inside the async task do not come back to this thread
// }

// Option1: exceptionally() converts a failure into a fallback value.
// CompletableFuture<Order> future =
//     CompletableFuture
//         .supplyAsync(() -> processOrder())
//         .exceptionally(ex -> {
//             log.error("Order failed", ex);
//             return fallbackOrder();
//         });

// Option2: handle() receives both possibilities: result or exception.
// future.handle((result, ex) -> {
//     if (ex != null) {
//         // handle failure
//     }
//     return result;
// });

// In our example:
// 1. runJob() catches its own failures and marks the JobRecord as FAILED.
// 2. runReport() keeps the CompletableFuture chain simple because these demo calls always succeed.
