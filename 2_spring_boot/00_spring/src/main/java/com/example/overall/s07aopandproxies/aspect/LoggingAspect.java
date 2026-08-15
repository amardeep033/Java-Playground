package com.example.overall.s7aopandproxies.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

// Yes - OrderController can become a proxy, but only if some Spring AOP advice/pointcut applies to OrderController.
// In this example the pointcuts target OrderService, so OrderService is proxied and OrderController stays a normal bean.

@Aspect
@Component
public class LoggingAspect {
    
    // Pointcut: only says where to intercept - it does not execute anything by itself.
    // execution(...) uses AspectJ pointcut expression syntax, which has its own pattern/wildcard rules.
    // Hardcoding the package path is normal practice when you want a narrow, explicit learning example.
    @Pointcut("execution(* com.example.overall.s7aopandproxies.service.OrderService.place*(..))")
    public void orderPlacementOperation() {
    }

    @Before("orderPlacementOperation()")
    public void logBeforeOrderPlacement(JoinPoint joinPoint) {
        System.out.println("L1. logBeforeOrderPlacement " + joinPoint.getSignature().getName());
    }

    // ------------

    @Pointcut("execution(* com.example.overall.s7aopandproxies.service.OrderService.calculateTotal(..))")
    public void totalCalculationOperation() {
    }


    // What to do on intercept - capture whatever is returned.
    // Here the return value is captured in "total"; the name must match returning = "total".
    // You can capture any returned value type; Object is flexible, or use a specific type such as int/Integer when you know it.
    // This advice runs only after normal return. If the target throws an exception, @AfterReturning will not run.
    @AfterReturning(pointcut = "totalCalculationOperation()", returning = "total")
    public void logAfterTotalCalculation(JoinPoint joinPoint, Object total) {
        System.out.println("L2. totalCalculationOperation " + joinPoint.getSignature().getName() + " returned " + total);
    }

    // ------------

    @Around("execution(* com.example.overall.s7aopandproxies.service.OrderService.*(..))")
    public Object log(ProceedingJoinPoint pjp) throws Throwable {

        System.out.println("Before");
        Object result = pjp.proceed(); // this is the main line of execution; it calls the actual target method
        System.out.println("After");

        return result;
    }
}

// Here the @Pointcut is reusable, while @AfterReturning consumes it.
// That means the pointcut method stores the "where" rule, and the advice annotation chooses "when/how" to run behavior for that rule.

// Pointcut-related:
// @Pointcut - defines a reusable pointcut expression
// @annotation(...) - matches methods with a specific annotation on the method
// @within(...) - matches join points where the declaring/containing class has a specific annotation
// execution(...) - matches method executions by return type, class/package, method name, and arguments
// within(...) - matches methods declared inside matching classes/packages by type/package pattern
// Note: execution, within, @annotation, etc. are pointcut expression designators, not Java annotations.

// Advice annotations:
// @Before
// @After
// @AfterReturning
// @AfterThrowing
// @Around
