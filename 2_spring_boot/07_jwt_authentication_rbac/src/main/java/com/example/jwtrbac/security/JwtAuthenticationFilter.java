package com.example.jwtrbac.security;

import com.example.jwtrbac.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// JWT pre-check filter: runs before controller code, similar to middleware.
// @Component only makes this class a Spring bean; it does NOT decide the filter order.
// The order comes from SecurityConfig: addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).
// Request path: servlet container -> Spring Security filter chain -> this JWT filter -> controller.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    //---------------------------------------------------------------------

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        // Important: missing token does not always mean return 401 here.
        // Public endpoints can continue; protected endpoints are rejected later by Spring Security rules.

        String token = header.substring("Bearer ".length());

        try {
            JwtService.JwtClaims claims = jwtService.validate(token, "access");

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(claims.subject());

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}

// SecurityContextHolder : Thread-local holder for current request security state.
// └── SecurityContext : Container object stored inside SecurityContextHolder.
//     └── Authentication : Current user's authentication result for this request.
//         ├── principal : Identity/user object; in this demo it is UserDetails(username=amar).
//         ├── authorities : Permissions/roles granted to the user, for example ROLE_USER, ORDER_READ.
//         ├── credentials : Secret used to authenticate, for example password/token; usually null after login.
//         ├── details : Extra request metadata, for example remote address or session id.
//         └── authenticated : Boolean flag telling Spring whether this request is already authenticated.

// Controller shortcuts:
// - Authentication parameter: use when you need username + authorities + auth metadata.
// - Principal parameter: use when you only need the current user's name.
// - @AuthenticationPrincipal: use when you need the principal object itself, usually UserDetails/domain user.

// Scope note:
// | @RequestScope | Creates your own bean once per HTTP request; useful for custom request data. |
// | SecurityContextHolder | Already stores current security state for the request thread, so we do not create a @RequestScope bean here. |
// | @SessionScope | Object lives across requests in an HTTP session; not used for stateless JWT access tokens. |

// Authorization model mapping:
// | Model | Example authority source |
// | RBAC | ROLE_USER, ROLE_ADMIN. |
// | ABAC | ownerId, department, region, account status. |
// | PBAC | Policy combines role + attributes + action + resource. |

// Use Authentication when you need username and authorities.
// Example: public Map<String, Object> me(Authentication authentication) { ... }
// Use @AuthenticationPrincipal when you need the full UserDetails/domain user object.
// Example: public Map<String, Object> me(@AuthenticationPrincipal UserDetails userDetails) { ... }
