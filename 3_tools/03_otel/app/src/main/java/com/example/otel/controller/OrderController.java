package com.example.otel.controller;

import com.example.otel.model.OrderResponse;
import com.example.otel.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/order")
    public ResponseEntity<OrderResponse> order(@RequestParam(defaultValue = "false") boolean fail) throws InterruptedException {
        try {
            orders.placeOrder(fail);
            return ResponseEntity.ok(OrderResponse.of("Coffee ordered"));
        } catch (IllegalStateException e) {
            return ResponseEntity.internalServerError().body(OrderResponse.of(e.getMessage()));
        }
    }
}

// HttpServletResponse and OrderResponse are different objects.
// Container creates HTTP response → Filter adds headers → Controller creates payload + chooses status → Spring writes status and JSON into HTTP response → Client receives it.

// Example response; transport headers such as Transfer-Encoding can vary.
// HTTP/1.1 200 OK
// X-Request-ID: 7c9e6679-7425-40de-944b-e07fc1f90ae7
// X-Correlation-ID: checkout-123
// X-Trace-ID: 1234567890abcdef1234567890abcdef
// Content-Type: application/json
// Transfer-Encoding: chunked
// Date: Thu, 29 Oct 2026 10:15:30 GMT
//
// {
//   "message": "Coffee ordered",
//   "requestId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
//   "traceId": "1234567890abcdef1234567890abcdef",
//   "correlationId": "checkout-123"
// }
