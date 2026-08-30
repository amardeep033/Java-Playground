package com.example.basiccrud.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(
        @NotBlank String idempotencyKey,
        @NotBlank String customerEmail,
        @NotBlank String sku,
        @Min(1) int quantity) {
}