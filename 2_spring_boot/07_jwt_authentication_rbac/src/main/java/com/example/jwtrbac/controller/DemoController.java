package com.example.jwtrbac.controller;

import com.example.jwtrbac.service.AdminService;
import java.security.Principal;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoController {

    private final AdminService adminService;

    public DemoController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/me")
    // Alternative when you need the actual principal object:
    // public Map<String, Object> me(@AuthenticationPrincipal UserDetails userDetails) { ... }
    public Map<String, Object> me(Authentication authentication) {
        return Map.of(
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities()
        );
    }

    // @PreAuthorize can express simple roles or richer business checks.
    // 1. hasRole('USER') | Role-based access; checks ROLE_USER. |
    // 2. hasAnyRole('ADMIN', 'SUPPORT') | At least one role must match. |
    // 3. hasAuthority('ORDER_READ') | Permission-style access; exact authority string. |
    // 4. hasAnyAuthority('ORDER_READ', 'ORDER_WRITE') | At least one authority must match. |
    // 5. hasRole('ADMIN') and hasAuthority('PRODUCT_DELETE') | Both conditions must pass. |
    // 6. hasRole('ADMIN') or principal.username == #username | Role OR current-user ownership check. |
    // 7. isAuthenticated() | Any logged-in user. |
    // 8. isAnonymous() | Only users without login/authentication. |
    // 9. permitAll / denyAll | Always allow / always block; usually cleaner at requestMatchers level. |
    // 10. @orderSecurity.canView(#id, authentication) | Custom bean check for ownership/business rules. |

    @GetMapping("/orders/my")
    @PreAuthorize("hasRole('USER')")
    public Map<String, String> myOrders(Principal principal) {
        return Map.of(
                "owner", principal.getName(),
                "orders", "demo order list"
        );
    }

    @DeleteMapping("/admin/products/{id}")
    public Map<String, String> deleteProduct(@PathVariable long id) {
        return adminService.deleteProduct(id);
    }
}
