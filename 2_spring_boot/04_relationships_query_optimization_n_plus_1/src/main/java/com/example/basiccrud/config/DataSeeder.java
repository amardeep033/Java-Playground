package com.example.basiccrud.config;

import com.example.basiccrud.model.Customer;
import com.example.basiccrud.model.CustomerOrder;
import com.example.basiccrud.model.OrderItem;
import com.example.basiccrud.repository.CustomerRepository;
import com.example.basiccrud.repository.OrderRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public DataSeeder(CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(String... args) {
        if (orderRepository.count() > 0) {
            return;
        }

        Customer ananya = customerRepository.save(new Customer(null, "Ananya Rao", "ananya@example.com"));
        Customer rohan = customerRepository.save(new Customer(null, "Rohan Mehta", "rohan@example.com"));
        Customer meera = customerRepository.save(new Customer(null, "Meera Iyer", "meera@example.com"));

        CustomerOrder order1 = new CustomerOrder(null, "ORD-1001", "PLACED", ananya);
        order1.addItem(new OrderItem(null, "Mechanical Keyboard", 1, new BigDecimal("4500.00")));
        order1.addItem(new OrderItem(null, "USB-C Cable", 2, new BigDecimal("350.00")));

        CustomerOrder order2 = new CustomerOrder(null, "ORD-1002", "PLACED", rohan);
        order2.addItem(new OrderItem(null, "Spring Boot Book", 1, new BigDecimal("800.00")));
        order2.addItem(new OrderItem(null, "Notebook", 3, new BigDecimal("80.00")));

        CustomerOrder order3 = new CustomerOrder(null, "ORD-1003", "PLACED", ananya);
        order3.addItem(new OrderItem(null, "Mouse", 1, new BigDecimal("900.00")));
        order3.addItem(new OrderItem(null, "Mouse Pad", 1, new BigDecimal("250.00")));

        CustomerOrder order4 = new CustomerOrder(null, "ORD-1004", "SHIPPED", meera);
        order4.addItem(new OrderItem(null, "JPA Interview Guide", 1, new BigDecimal("700.00")));
        order4.addItem(new OrderItem(null, "Pen", 5, new BigDecimal("10.00")));

        orderRepository.save(order1);
        orderRepository.save(order2);
        orderRepository.save(order3);
        orderRepository.save(order4);
    }
}
