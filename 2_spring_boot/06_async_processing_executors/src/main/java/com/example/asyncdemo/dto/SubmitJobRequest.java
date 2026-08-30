package com.example.asyncdemo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SubmitJobRequest(
        @NotBlank
        String description,

        @Min(1)
        @Max(10)
        int seconds
) {
}
