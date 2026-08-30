package com.example.basiccrud.controller;

import com.example.basiccrud.dto.CheckoutRequest;
import com.example.basiccrud.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Keep transaction boundaries out of the controller.
// Controller should only translate HTTP -> Java call.
// Service owns the business use case, so it should decide what must commit/rollback together.
// This keeps transaction behavior reusable even if the same checkout is called from a job, listener, or another API.
@RestController
@RequestMapping("/api")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout")
    public CheckoutService.CheckoutResult checkout(@Valid @RequestBody CheckoutRequest request) {
        return checkoutService.checkout(
                request.idempotencyKey(),
                request.customerEmail(),
                request.sku(),
                request.quantity());
    }
}
