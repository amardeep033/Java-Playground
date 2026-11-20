package com.eventrelay.servicea.endpoint.dto;



public record IssuedEndpoint(WebhookEndpoint endpoint, String signingSecret) {
    @Override
    public String toString() {
        return "IssuedEndpoint[signing secret redacted]";
    }
}
