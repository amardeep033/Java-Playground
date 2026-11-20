package com.eventrelay.servicea.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class UpdateEndpoint {
    @Size(min = 1, max = 120)
    private String name;
    @Size(max = 500)
    private String description;
    @Valid
    private EndpointDeliveryConfiguration configuration;
    private boolean descriptionProvided;

    @JsonSetter(nulls = Nulls.FAIL)
    public void setName(String value) {
        name = value;
    }

    @JsonSetter
    public void setDescription(String value) {
        description = value;
        descriptionProvided = true;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setConfiguration(EndpointDeliveryConfiguration value) {
        configuration = value;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public EndpointDeliveryConfiguration configuration() {
        return configuration;
    }

    @JsonIgnore
    public boolean hasDescription() {
        return descriptionProvided;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isValidUpdate() {
        return (name != null || descriptionProvided || configuration != null) && (name == null || !name.isBlank());
    }

    @JsonAnySetter
    public void reject(String field, Object value) {
        throw new IllegalArgumentException("Only name, description and delivery configuration are allowed");
    }
}
