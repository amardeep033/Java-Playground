package dev.playground.springbootgradle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class HelloSpringBootGradleApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloSpringBootGradleApplication.class, args);
    }
}

@RestController
class HelloController {

    @GetMapping("/hello")
    String hello() {
        return "Hello from Spring Boot with Gradle";
    }
}
