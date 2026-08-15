package com.example.overall.s8validation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@Configuration
@ComponentScan("com.example.overall.s8validation")
public class AppConfig {

    // This is the Bean Validation engine used by @Valid, @NotBlank, @Email, @Size, and friends.
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    // Plain Spring needs this bean for method-parameter validation.
    // In a real MVC controller, @Valid @RequestBody is wired by Spring MVC before the method body runs.
    @Bean
    public static MethodValidationPostProcessor methodValidationPostProcessor() {
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        return postProcessor;
    }
}
