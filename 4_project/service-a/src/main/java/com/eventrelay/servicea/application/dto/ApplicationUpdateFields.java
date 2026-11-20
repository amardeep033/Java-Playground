package com.eventrelay.servicea.application.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.*;

public class ApplicationUpdateFields {
    @Size(max = 500)
    private String description;
    private Boolean active;
    private boolean descriptionProvided;

    @JsonSetter
    public void setDescription(String value) {
        description = value;
        descriptionProvided = true;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setActive(Boolean value) {
        active = value;
    }

    public String description() {
        return description;
    }

    public Boolean active() {
        return active;
    }

    @JsonIgnore
    public boolean hasDescription() {
        return descriptionProvided;
    }

    protected boolean hasName() {
        return false;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isValidUpdate() {
        return hasName() || descriptionProvided || active != null;
    }

    @JsonAnySetter
    public void reject(String field, Object value) {
        throw new IllegalArgumentException("Unsupported resource update field");
    }
}
