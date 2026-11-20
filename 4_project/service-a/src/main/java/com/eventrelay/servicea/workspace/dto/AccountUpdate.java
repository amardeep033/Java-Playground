package com.eventrelay.servicea.workspace.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public record AccountUpdate(@Size(min = 1, max = 120) String name, @Email @Size(max = 320) String email,
        @Pattern(regexp = "USER|ADMIN") String role) {
    @AssertTrue
    public boolean isValidUpdate() {
        return (name != null || email != null || role != null) && (name == null || !name.isBlank())
                && (email == null || !email.isBlank());
    }

    @JsonAnySetter
    public void reject(String field, Object value) {
        throw new IllegalArgumentException("Only name, email and role can be updated");
    }
}
