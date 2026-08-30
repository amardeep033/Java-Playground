package com.example.asyncdemo.dto;

import java.time.Instant;

import com.example.asyncdemo.model.JobStatus;

public record JobResponse(
        String id,
        String description,
        JobStatus status,
        String result,
        String error,
        Instant createdAt,
        Instant updatedAt
) {
}
