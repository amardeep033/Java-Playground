package com.eventrelay.servicea.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

@Configuration
public class CredentialConfig {
    // Because PasswordEncoder is a library interface, and Pbkdf2PasswordEncoder is a library class that we don't control.
    // Component scanning discovers annotated classes. @Bean lets you explicitly tell Spring how to create an object and register it as a bean.
    @Bean
    PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
