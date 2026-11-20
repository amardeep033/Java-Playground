package com.eventrelay.servicea.eventtype.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public record UpdateDescription(@Size(max = 500) String description) {
    @JsonAnySetter
    public void reject(String field, Object value) {
        throw new IllegalArgumentException("Only description can be updated");
    }
}
