package com.eventrelay.servicea.eventtype.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public record Activation(@NotNull Boolean active) {
    @JsonAnySetter
    public void reject(String field, Object value) {
        throw new IllegalArgumentException("Only active can be updated");
    }
}
