package com.example.jwtrbac.service;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;


@Service
public class AdminService {
    // Method-level authorization examples:
    // 1. hasRole('ADMIN') | Role-based access; checks ROLE_ADMIN. |
    // 2. hasAnyRole('ADMIN', 'SUPPORT') | At least one role must match. |
    // 3. hasAuthority('PRODUCT_WRITE') | Permission-style access; exact authority string. |
    // 4. hasAnyAuthority('PRODUCT_READ', 'PRODUCT_WRITE') | At least one authority must match. |
    // 5. hasRole('ADMIN') and hasAuthority('PRODUCT_DELETE') | Both conditions must pass. |
    // 6. hasRole('ADMIN') or principal.username == #username | Role OR current-user ownership check. |
    // 7. isAuthenticated() | Any logged-in user. |
    // 8. isAnonymous() | Only users without login/authentication. |
    // 9. permitAll / denyAll | Always allow / always block; usually cleaner at requestMatchers level. |
    // 10. @orderSecurity.canView(#id, authentication) | Custom bean check for ownership/business rules. |

    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> deleteProduct(long id) {
        return Map.of(
                "status", "deleted",
                "productId", String.valueOf(id)
        );
    }
}
