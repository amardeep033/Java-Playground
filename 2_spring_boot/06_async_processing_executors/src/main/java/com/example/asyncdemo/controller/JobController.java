package com.example.asyncdemo.controller;

import com.example.asyncdemo.dto.JobResponse;
import com.example.asyncdemo.dto.SubmitJobRequest;
import com.example.asyncdemo.service.JobService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService asyncJobService;

    public JobController(JobService asyncJobService) {
        this.asyncJobService = asyncJobService;
    }

    //---------------------------------------------------------------

    @PostMapping
    public ResponseEntity<JobResponse> submitJob(@Valid @RequestBody SubmitJobRequest request) {
        JobResponse response = asyncJobService.submitJob(request);
        return ResponseEntity
                .accepted()
                .location(URI.create("/api/jobs/" + response.id()))
                .body(response);
    }

    //---------------------------------------------------------------

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable String id) {
        return asyncJobService.findJob(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    //---------------------------------------------------------------

    @GetMapping
    public List<JobResponse> listJobs() {
        return asyncJobService.listJobs();
    }

    //---------------------------------------------------------------

    @GetMapping("/report")
    public CompletableFuture<ResponseEntity<String>> report() {
        return asyncJobService.runReport().thenApply(ResponseEntity::ok);
    }
}

// | setWaitForTasksToCompleteOnShutdown(true) | Spring setting: wait for submitted tasks during graceful shutdown. |
// | shutdown() | ExecutorService operation: stop accepting new tasks and let already submitted tasks finish. |
