package com.example.jwtrbac.config;

import com.example.jwtrbac.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                // CORS and CSRF are HTTP/browser concerns configured at the SecurityFilterChain level.
                // This demo disables CSRF because it uses stateless Bearer tokens in the Authorization header.
                // If JWT is stored in cookies, revisit CSRF protection because browsers attach cookies automatically.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Request-level authorization happens before the controller method is called.
                        // 1. requestMatchers | URL / HTTP route | /api/admin/** requires ADMIN. |
                        // 2. @PreAuthorize | Method / business rule | hasRole, hasAuthority, hasAnyRole, ownership checks. |
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // Add the JWT filter before UsernamePasswordAuthenticationFilter because JWT authentication should populate SecurityContext before Spring checks route/method authorization.
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter jwtFilter) {
        // The JWT filter is added inside Spring Security with addFilterBefore(...).
        // Disable normal servlet auto-registration so it does not run a second time outside the security chain.
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(jwtFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // UserDetailsService is Spring Security's user lookup abstraction.
        // In this demo: InMemoryUserDetailsManager returns hardcoded users.
        // In real code: CustomUserDetailsService implements UserDetailsService and loads users from DB.
        return new InMemoryUserDetailsManager(
                User.withUsername("user")
                        .password(passwordEncoder.encode("password"))
                        .roles("USER")
                        .build(),
                User.withUsername("admin")
                        .password(passwordEncoder.encode("password"))
                        .roles("USER", "ADMIN")
                        .build()
        );
    }

    // Password storage mental model:
    // 1. Hashing, not encryption: password -> hash is one-way that is hash -> password should not be possible.
    // 2. Salt: BCrypt stores a random salt inside the final hash so equal passwords do not produce equal hashes.
    // 3. Cost factor: BCrypt is deliberately slow; higher cost means more work for attackers and servers.
    // 4. Verification: PasswordEncoder extracts algorithm + cost + salt from stored hash, hashes the entered password, then compares the newly produced hash with the stored hash.
    
    // BCrypt hash anatomy: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
    // | Version | $2a$ | BCrypt version / algorithm identifier. |
    // | Cost | 10 | Work factor; roughly 2^10 hashing rounds. |
    // | Salt | N9qo8uLOickgx2ZMRZoMye | 22-character Base64 salt stored with the hash. |
    // | Hash | IjZAgcfl7p92ldGxad68LJZdL17lhWy | Result of hashing password + salt + cost. |

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}

// 1. CORS | Can JavaScript from another origin read this API response? | Browser data exposure across origins. | Needed when frontend and API run on different origins. |
// 2. CSRF | Can another site cause the victim's browser to send an authenticated request? | Unwanted state-changing action. | Mainly a risk when auth is stored in cookies, because browsers attach cookies automatically. |
// Origin = scheme + host + port, for example https://example.com:443.
// CORS is enforced by browsers, not by backend-to-backend service calls.
// Bearer JWT in Authorization header lowers CSRF risk because another site cannot automatically add that header.
// JWT in cookies can still have CSRF risk; then use SameSite, CSRF tokens, and safe method design.
// Set-Cookie: refresh_token=abc;
//              Secure;    // send only over HTTPS
//              HttpOnly;  // JavaScript cannot read it
//              SameSite=Lax; // Strict/Lax/None; SameSite=None also requires Secure
