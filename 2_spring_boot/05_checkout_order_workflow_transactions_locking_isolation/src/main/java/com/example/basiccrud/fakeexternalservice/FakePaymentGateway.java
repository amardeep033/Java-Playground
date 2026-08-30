package com.example.basiccrud.fakeexternalservice;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class FakePaymentGateway {

    public boolean charge(String orderNumber, String idempotencyKey, BigDecimal amount) {
        // Placeholder for an external payment provider.
        // Checkout directly depends on this result, unlike analytics/email/warehouse events.
        //
        // Real payment call rules:
        // - send an idempotency key to the provider
        // - retry only retryable failures like timeout/5xx
        // - do not retry permanent failures like insufficient funds
        // - use backoff, not tight loops
        // Note: this always returns true, so the FAILED/compensation branch in CheckoutService never runs in this demo.
        // A real retry loop has a failure condition, backoff between attempts, and stops on permanent errors.
        for (int attempt = 1; attempt <= 2; attempt++) {
            System.out.println("[PAYMENT] attempt=" + attempt + " order=" + orderNumber + " amount=" + amount);
        }
        return true;
    }
}
