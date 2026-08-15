package com.example.overall.s2autoconfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// @Configuration: this class still tells Spring how to start the container configuration.
// @ComponentScan: tells Spring to scan this package and discover classes annotated with @Component.
// Difference from s1manualconfig: we do not write @Bean methods for FileLogger and OrderService here.
// Spring Boot makes using Spring components easier by providing defaults such as component scanning through @SpringBootApplication.
// @SpringBootApplication combines @SpringBootConfiguration, @EnableAutoConfiguration, and @ComponentScan.
// @SpringBootConfiguration itself is a Spring @Configuration specialization.

// We still need some configuration entry point so plain Spring knows where to start.
@Configuration
// Component scanning starts from package com.example.overall.s2autoconfig and scans that package plus its subpackages.
// This class is empty because the bean definitions are discovered from @Component classes instead of being written as @Bean methods.
@ComponentScan
public class AppConfig {
}

// Could you avoid an AppConfig.java file? Technically yes, by putting configuration on Main:
// @Configuration
// @ComponentScan
// public class Main {
//     public static void main(String[] args) {
//         try (var context = new AnnotationConfigApplicationContext(Main.class)) {
//             OrderService orderService = context.getBean(OrderService.class);
//             orderService.placeOrder();
//         }
//     }
// }
