package com.example.asyncdemo.model;

import java.time.Instant;

import com.example.asyncdemo.dto.JobResponse;

public class JobRecord {

    private final String id;
    private final String description;
    private final Instant createdAt;
    private volatile JobStatus status;
    private volatile String result;
    private volatile String error;
    private volatile Instant updatedAt;

    public JobRecord(String id, String description) {
        this.id = id;
        this.description = description;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.status = JobStatus.ACCEPTED;
    }

    public String id() {
        return id;
    }

    public void markRunning() {
        this.status = JobStatus.RUNNING;
        this.updatedAt = Instant.now();
    }

    public void markSucceeded(String result) {
        this.status = JobStatus.SUCCEEDED;
        this.result = result;
        this.error = null;
        this.updatedAt = Instant.now();
    }

    public void markFailed(Exception exception) {
        this.status = JobStatus.FAILED;
        this.error = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        this.updatedAt = Instant.now();
    }

    public JobResponse toResponse() {
        return new JobResponse(id, description, status, result, error, createdAt, updatedAt);
    }
}
