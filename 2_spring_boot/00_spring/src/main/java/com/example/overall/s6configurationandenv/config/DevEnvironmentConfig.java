package com.example.overall.s6configurationandenv.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// application-dev.properties is a special Boot filename and loads automatically for the dev profile.
// The class name DevEnvironmentConfig is ordinary; @Profile("dev") controls registration.
@Configuration
@Profile("dev")
public class DevEnvironmentConfig {
}
