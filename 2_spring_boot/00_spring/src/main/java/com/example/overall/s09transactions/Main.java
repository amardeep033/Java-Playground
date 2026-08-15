package com.example.overall.s9transactions;

import com.example.overall.s9transactions.config.AppConfig;
import com.example.overall.s9transactions.controller.OrderController;
import com.example.overall.s9transactions.repository.OrderRepository;
import com.example.overall.s9transactions.service.OrderService;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

    // A transaction does not always mean "database transaction" in general computing.
    // But Spring's @Transactional is mainly for transactional resources such as databases, JMS, or other resources with transaction managers.
    // Simple non-DB example: if you create/delete a file, Spring will not automatically undo the file operation on rollback.
    // It is not magic: @Transactional does not know how to undo arbitrary Java side effects such as sending email or writing files.
    // For non-transactional side effects, design compensation manually or run them after commit using events.

public class Main {
    public static void main(String[] args) {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            OrderController orderController = context.getBean(OrderController.class);
            OrderRepository orderRepository = context.getBean(OrderRepository.class);
            OrderService orderService = context.getBean(OrderService.class);

            System.out.println("--------------------------------------------------------");
            System.out.println("M1. orderService: proxy " + AopUtils.isAopProxy(orderService));
            orderController.placeOrder();
            orderRepository.printRows();
        }
    }
}
