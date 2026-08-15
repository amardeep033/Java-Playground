package com.example.overall.s7aopandproxies.caller;

import com.example.overall.s7aopandproxies.service.OrderService;
import org.springframework.stereotype.Controller;

// Caller bean: it receives the Spring-managed OrderService bean, which is a proxy.
// OrderController itself is not a proxy here because no advice matches OrderController methods.
@Controller
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public void placeOrderFromOutside() {
        System.out.println("C1. placeOrderFromOutside");
        orderService.placeOrderE("outside_item");
    }

    public void placeOrderWithSelfInvocation() {
        System.out.println("C2. placeOrderWithSelfInvocation");
        orderService.placeOrderI("inside_item");
    }
}

// In self invocation -- outer object is proxied but inner fn call are direct without outer interference therefore not proxied.
// For example here:
// OrderController -> OrderService proxy -> placeOrderI(...) advice can run.
// Inside placeOrderI, this.calculateTotal(...) is OrderService target -> OrderService target, so calculateTotal advice does not run.
