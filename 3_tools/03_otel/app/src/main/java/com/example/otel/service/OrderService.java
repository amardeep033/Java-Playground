package com.example.otel.service;

import com.example.otel.repository.InventoryRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

// On application startup:
// The OpenTelemetry starter initializes its SDK and tracer provider during application startup.
// Spring injects the OpenTelemetry instance into OrderService.
// getTracer("order-demo") obtains a Tracer associated with that instrumentation scope.
// You store it in this.tracer and reuse it.

@Service
public class OrderService {
    // Logs, traces, and metrics.
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final Tracer tracer;
    private final MeterRegistry meters;

    private final InventoryRepository inventory;

    public OrderService(InventoryRepository inventory, MeterRegistry meters, OpenTelemetry telemetry) {
        // No request log in the constructor because it runs at bean creation, normally before any request/span context exists; startup logging is still allowed.
        this.inventory = inventory;
        this.meters = meters;
        this.tracer = telemetry.getTracer("order-demo"); // Instrumentation scope name: identifies the code producing these spans. This Tracer is obtained once per service instance and reused across requests; the scope is metadata, not a metric label.
    }

    //fn0
    public void placeOrder(boolean fail) throws InterruptedException {
        Timer.Sample sample = Timer.start(meters);

        Span order = tracer.spanBuilder("place-order").startSpan();
        String outcome = "success";
        try (Scope ignored = order.makeCurrent()) {
            order.setAttribute("order.item", "coffee");
            try {
                checkStock(); //fn1
                takePayment(fail); //fn2
                log.atInfo().addKeyValue("event", "order.completed").log("Coffee ordered");
            } catch (RuntimeException | InterruptedException e) {
                outcome = "error";
                order.recordException(e);
                order.setStatus(StatusCode.ERROR, e.getMessage());
                log.atWarn().addKeyValue("event", "order.failed").log(e.getMessage());
                throw e;
            }
        } finally {
            // MeterRegistry: each name + outcome tag identifies one meter, created on first use here.
            // +-- "orders.requests"
            // |       +-- outcome=success → Counter
            // |       +-- outcome=error   → Counter
            // +-- "orders.duration"
            // |       +-- outcome=success → Timer with histogram buckets
            // |       +-- outcome=error   → Timer with histogram buckets
            // +-- "orders.latency"
            //         +-- outcome=success → Timer with local p50/p95
            //         +-- outcome=error   → Timer with local p50/p95
            meters.counter("orders.requests", "outcome", outcome).increment();
            long nanos = sample.stop(meters.timer("orders.duration", "outcome", outcome));
            meters.timer("orders.latency", "outcome", outcome).record(nanos, TimeUnit.NANOSECONDS);

            order.end();
        }
    }

    //fn1
    private void checkStock() throws InterruptedException {
        // A trace contains one or more spans (individual operations), and these spans can be nested.
        // For each operation we choose to trace, we start a named span; not every function needs a span.
        // If a valid parent span is current, the new span becomes its child with a new span ID and the same trace ID; otherwise, it starts a new trace.
        Span span = tracer.spanBuilder("check-stock").startSpan(); // Creates a new span; it is not yet current.
        try (Scope ignored = span.makeCurrent()) { // Makes this span current; closing the scope restores the previous context.
            Thread.sleep(80);
            span.setAttribute("stock.available", inventory.available("coffee"));
            log.atInfo().addKeyValue("event", "stock.checked").log("Stock checked");
        } finally {
            // Scope.close() has already restored the previous context, containing the parent span here.
            span.end(); // Ends the new span, which is no longer current here.
        }
    }

    //fn2
    private void takePayment(boolean fail) throws InterruptedException {
        Span span = tracer.spanBuilder("take-payment").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            try {
                Thread.sleep(fail ? 250 : 120);
                if (fail) throw new IllegalStateException("Demo payment declined");
                log.atInfo().addKeyValue("event", "payment.accepted").log("Payment accepted");
            } catch (RuntimeException | InterruptedException e) {
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, e.getMessage());
                throw e;
            }
        } finally {
            span.end();
        }
    }
}

// Multiple nested operations can execute on the same thread, with scopes changing and restoring the current context.
// Example grouping of this service's manual spans; a trace can span multiple services and instrumentation scopes.
// Service: otel-demo (from application.properties: otel.service.name=otel-demo)
//     +-- Instrumentation scope: order-demo (from telemetry.getTracer("order-demo"))
//             +-- Trace ID: 1234567890abcdef1234567890abcdef // No independent trace-name field in OTel; UIs may derive a display name from a span.
//                     +-- Span: place-order (from tracer.spanBuilder("place-order"))
//                     |       +-- Span: check-stock (from tracer.spanBuilder("check-stock"))
//                     |       +-- Span: take-payment (from tracer.spanBuilder("take-payment"))
