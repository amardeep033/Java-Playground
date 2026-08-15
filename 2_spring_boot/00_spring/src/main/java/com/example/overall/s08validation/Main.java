package com.example.overall.s8validation;

import com.example.overall.s8validation.config.AppConfig;
import com.example.overall.s8validation.controller.OrderController;
import com.example.overall.s8validation.dto.OrderRequest;
import com.example.overall.s8validation.error.ValidationErrorFormatter;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


// Model: Controller(DTO) -> Service(Domain Model) -> Repository(Entity).
// DTO request validation uses @Valid to trigger rules such as @NotNull, @NotBlank, @Size, and @Email.
// Bad code: if (name == null || name.isBlank()) in service -- the service should not repeat basic boundary/input validation.

public class Main {
    public static void main(String[] args) {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            OrderController orderController = context.getBean(OrderController.class);
            ValidationErrorFormatter errorFormatter = context.getBean(ValidationErrorFormatter.class);

            try {
                orderController.placeOrder(
                    new OrderRequest(
                        10,                // ✅ @NotNull
                        "hello",           // ✅ @NotBlank
                        "abcd",            // ✅ @Size
                        null,              // ✅ @Email — null is allowed
                        "good@gmail.com"   // ✅ @NotBlank + @Email
                    )
                );
            } catch (ConstraintViolationException exception) {
                System.out.println(errorFormatter.format(exception));
            }
        }
    }
}

// Validation failure produces an exception.
// In Spring MVC, @Valid @RequestBody commonly fails with MethodArgumentNotValidException.
// In this console method-validation example, @Valid on the method parameter fails with ConstraintViolationException.
// Instead of putting error handling in every controller, API code usually handles validation errors globally.
// In API code:
// @ControllerAdvice
// public class GlobalExceptionHandler {
//     @ExceptionHandler(MethodArgumentNotValidException.class)
//     public ResponseEntity<?> handleValidation(
//             MethodArgumentNotValidException ex) {

//         // build error response

//         return ResponseEntity.badRequest().body(...);
//     }
// }

// ControllerAdvice: is this like advice in AOP but special, without pointcut, where pointcut becomes all controllers?
// Close mental connection, but not technically AOP.
// @ControllerAdvice is Spring MVC infrastructure for controller-level cross-cutting behavior such as exception handling.
// It does not create AOP proxies and it does not use AspectJ pointcut expressions.
// Think "global MVC exception handler for controllers", not "AOP advice around controller methods".

// POST /users
//      ↓
// JSON → DTO
//      ↓
// @Valid
//      ↓
// Validation fails
//      ↓
// MethodArgumentNotValidException
//      ↓
// @ControllerAdvice
//      ↓
// @ExceptionHandler
//      ↓
// 400 + clean error response
