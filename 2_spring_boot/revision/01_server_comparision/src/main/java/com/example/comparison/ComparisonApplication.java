package com.example.comparison;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 1. WebConfig.java: enable MVC and discover our Spring beans.
// 2. WebInitializer.java: register Spring's DispatcherServlet with the server.

// 0. SPRING BOOT: component scanning + auto-configuration + embedded server startup.
@SpringBootApplication
public class ComparisonApplication {
    public static void main(String[] args) {
        SpringApplication.run(ComparisonApplication.class, args);
    }
}

// 1. without spring: manual DI + from outside(maybe concrete or interface) + in all .java file
// 2. with spring(manual): wiring moved to single file + bean (spring managed object lifecycle) + context.getBean(Service.class)
// 3. with spring(auto): auto map making use of component scan and component + but still context.getBean(Service.class)(not needed in case of web?)

// 4. with springboot:
// - no config file, only components directly + no need of context.getBean(Service.class)
// - but it has major benefits in configure MVC + app init + DispatcherServlet + servlet container such as Tomcat
// - Spring Boot = Automatic setup + sensible defaults + easier execution/deployment

// Area       | Spring Framework                                | What Spring Boot adds
// -----------+-------------------------------------------------+----------------------------------------------------
// Beans / DI | Creates, injects, manages beans                 | Auto-configures many infrastructure beans
// Web / MVC  | Controllers, DispatcherServlet, request mapping | Embedded server, default MVC and JSON configuration
// Database   | JDBC, transactions, Spring Data integration     | DataSource and JPA auto-configuration
// Security   | Authentication and authorization framework      | Default security configuration
// Monitoring | Infrastructure support                          | Actuator endpoints and health checks




// client -> tomcat(servlet container)[dispatcher servlet -> controller -> service]
// tomcat: Runs an HTTP server and manages servlets
// Servlet: Handles requests and responses through a Java API
// DispatcherServlet: Spring MVC servlet that routes requests to controllers

// @WebServlet("/hello")
// public class HelloServlet extends HttpServlet { //HttpServlet is a class provided by the Jakarta Servlet API.
//     @Override
//     protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
//         response.setContentType("text/plain");
//         response.getWriter().write("Hello World");
//     }
// }

// @RestController
// public class HelloController {
//     @GetMapping("/hello")
//     public String hello() {
//         return "Hello World";
//     }
// }
