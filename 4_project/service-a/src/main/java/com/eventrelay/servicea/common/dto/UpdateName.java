package com.eventrelay.servicea.common.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public record UpdateName(@NotBlank @Size(max = 120) String name) {
    @JsonAnySetter
    public void reject(String field, Object value) {
        throw new IllegalArgumentException("Only name can be updated");
    }
}
