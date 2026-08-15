package com.example.overall.s10events;

import com.example.overall.s10events.config.AppConfig;
import com.example.overall.s10events.service.OrderService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

// If placing an order also needs email, audit, inventory, analytics, etc.,
// putting all those calls directly inside placeOrder can pollute the main use-case flow.
// One option is an orchestrator service, but then that orchestrator must know and coordinate every side effect.
// Events help when we want: 1. placeOrder to stay focused, and 2. side effects to be easy to add/remove independently.
// Mental model: Create event -> Publish event -> Listen/react.

public class Main {
    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            OrderService orderService = context.getBean(OrderService.class);

            System.out.println("M1. Main -> Service");
            orderService.placeOrder();
        }
    }
}

// @TransactionalEventListener is transaction-lifecycle-aware.
// Common example: send email only after the order DB transaction commits successfully.
// Different phases:
// @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) -- default; run only after successful commit.
// @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK) -- run only after rollback.
// @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION) -- run after commit or rollback.
// @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT) -- run before commit completes.

// IMP: If there is no active transaction, @TransactionalEventListener does not run by default
// because there is no transaction phase to wait for. Use fallbackExecution = true only when that behavior is intentional.
// @Order works with @TransactionalEventListener too; it orders listeners within the same transaction phase.
