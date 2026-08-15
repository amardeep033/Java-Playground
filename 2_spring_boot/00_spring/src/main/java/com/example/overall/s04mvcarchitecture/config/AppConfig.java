package com.example.overall.s4mvcarchitecture.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
// AppConfig is inside the config subpackage, so the parent package is specified explicitly to scan controller, service, repository, and other sibling packages.
@ComponentScan("com.example.overall.s4mvcarchitecture")
public class AppConfig {
}
