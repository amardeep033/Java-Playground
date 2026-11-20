package com.eventrelay.servicea.apikey.dto;



public record IssuedApiKey(ApiKeyDetails key, String secret) {
    @Override
    public String toString() {
        return "IssuedApiKey[secret redacted]";
    }
}
