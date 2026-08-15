package com.example.overall.s2autoconfig;

// This abstraction is exactly the same as in s0 and s1.
// Interfaces are not components by themselves because Spring needs a concrete class to instantiate.
public interface LoggerService {
    void log(String message);
}

// If multiple classes implement this interface and all are Spring beans, Spring needs a clear selection using @Primary or @Qualifier.
// If there is no unique candidate, Spring throws NoUniqueBeanDefinitionException when it tries to resolve the dependency.

// @Component
// @Primary
// class StripePaymentProcessor implements PaymentProcessor {
// }

// @Component("stripe")
// class StripePaymentProcessor implements PaymentProcessor {
// }
