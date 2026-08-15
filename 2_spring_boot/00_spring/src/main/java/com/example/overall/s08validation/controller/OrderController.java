package com.example.overall.s8validation.controller;

import com.example.overall.s8validation.dto.OrderRequest;
import com.example.overall.s8validation.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

// @Validated enables Spring method validation on this bean.
// @Valid on the parameter says: validate the DTO object before running the method body.
@Controller
@Validated // Enables method validation here; validation groups are an advanced use of @Validated.
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Put @Valid on the controller parameter -- triggers Bean Validation on an object, typically a request DTO.
    public void placeOrder(@Valid OrderRequest request) {
        System.out.println("C1. Controller accepted DTO");
        orderService.placeOrder(request);
    }
}

// In API code:
// @PostMapping("/users")
// public UserResponse create(@Valid @RequestBody CreateUserRequest request)
// @RequestBody means Spring MVC reads the HTTP request body, usually JSON, and converts it into the DTO object.
// @Valid then validates that DTO before the controller method body executes.

// |                          | `@Valid`           | `@Validated`                              |
// | ------------------------ | ------------------ | ----------------------------------------- |
// | Provided by              | Jakarta Validation | Spring                                    |
// | Typical use              | Validate DTO       | Validation + Spring features              |
// | Nested object validation | ✅                 | ✅                                        |
// | Validation groups        | ❌                 | ✅                                        |
// | Common in Spring Boot    | **Very common**    | Common when advanced validation is needed |

// What is Jakarta?
// Jakarta EE is the successor to Java EE. Many enterprise APIs moved from javax.* packages to jakarta.* packages.
// Bean Validation annotations such as jakarta.validation.Valid and jakarta.validation.constraints.NotBlank come from that standard API.
// Spring uses those standard annotations and plugs in an implementation such as Hibernate Validator.

// @Validated -- This can be used for method/parameter validation, such as constraints on method parameters.
// @RestController
// public class UserController {
