package com.eventrelay.servicea.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.*;

public class UpdateApplication extends ApplicationUpdateFields {
    @Size(min = 1, max = 120)
    private String name;

    @JsonSetter(nulls = Nulls.FAIL)
    public void setName(String value) {
        name = value;
    }

    public String name() {
        return name;
    }

    @Override
    protected boolean hasName() {
        return name != null;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isValidName() {
        return name == null || !name.isBlank();
    }
}
