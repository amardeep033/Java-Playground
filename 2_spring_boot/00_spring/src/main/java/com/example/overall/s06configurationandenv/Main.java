package com.example.overall.s6configurationandenv;

import com.example.overall.s6configurationandenv.config.AppConfig;
import com.example.overall.s6configurationandenv.service.OrderService;
import java.util.Arrays;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

public class Main {
    public static void main(String[] args) {
        // Pass the profile as a command-line argument:
        // mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s6configurationandenv.Main -Dexec.args="--spring.profiles.active=dev"
        // mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s6configurationandenv.Main -Dexec.args="--spring.profiles.active=prod"
        SpringApplication application = new SpringApplication(AppConfig.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);

        try (ConfigurableApplicationContext context = application.run(args)) {
            // Command-line, JVM, and OS-environment values can override values from config files.
            System.out.println("Active profile: "
                    + Arrays.toString(context.getEnvironment().getActiveProfiles()));

            OrderService orderService = context.getBean(OrderService.class);
            orderService.placeOrder();
        }
    }
}

// dev chain:
// --spring.profiles.active=dev -> application.properties + application-dev.properties
// -> @Profile("dev") -> DevLogger -> individual @Value fields
//
// prod chain:
// --spring.profiles.active=prod -> application.properties -> imports profile-activated app-prod.yaml
// -> @Profile("prod") -> ProdLogger -> grouped @ConfigurationProperties
//
// One active profile makes two parallel decisions: it selects profile-specific property values
// and registers profile-specific beans. Both decisions meet when Spring constructs OrderService.
//
// Common ways to activate a Spring Boot profile:
// 1. Command-line argument: --spring.profiles.active=dev
// 2. JVM system property: -Dspring.profiles.active=dev
// 3. OS environment variable: SPRING_PROFILES_ACTIVE=dev
// 4. Programmatically: application.setAdditionalProfiles("dev")
// For this example, use the command-line argument so switching profiles is visible at run time.

// Important boundary: s0-s5 use plain Spring. This s6 example starts with SpringApplication because
// automatic application/profile file loading and @ConfigurationProperties binding are Boot features.
// @Configuration, @Bean, @Value, Environment, and @Profile themselves are Spring Framework features.
