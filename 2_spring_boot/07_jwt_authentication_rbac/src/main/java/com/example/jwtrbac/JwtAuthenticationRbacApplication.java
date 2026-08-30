package com.example.jwtrbac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

// IAM | Users + service-to-service identity/access. | User login, API clients, service credentials. |
// UAM | User access management only. | User roles, groups, permissions. |

// External client -> API | OAuth 2.0 / OIDC + JWT access token. |
// Service -> Service | OAuth 2.0 Client Credentials + JWT, or mTLS. |
// Service -> External provider | OAuth 2.0 Client Credentials or API key, depending on provider. |
// Browser user -> Service | Session cookie, JWT, or OIDC login. |

// Human | Password + session/cookie, or OIDC. |
// App/API client | API key, JWT, PAT, OAuth token. |
// Machine/service | mTLS certificate, SSH key, workload identity. |

// OAuth = authorization framework.
// OIDC = OpenID Connect; authentication layer built on top of OAuth 2.0.

// Authentication = "Who are you?" Failure usually returns 401 Unauthorized.
// Authorization = "What are you allowed to do?" Failure usually returns 403 Forbidden.

// Session stateful | Cookie containing session id | Session store/cache/DB | Easy logout/revocation; server-side state required. |
// Token stateless | JWT access token | No access-token lookup if JWT has needed claims | Scales well; token theft/revocation need care. |

// RBAC | Role | ADMIN, USER. |
// ABAC | Attributes | department=finance, ownerId=request.userId, region=IN. |
// PBAC | Policy rules | allow if role=MANAGER and amount<50000 and region matches. |
// Permission/scopes | Fine-grained authority | PRODUCT_READ, ORDER_CANCEL, USER_DELETE. |

// In this example:
// 1. Libraries used: Spring Security for auth/RBAC + manual JWT signing/validation with Java crypto.
// 2. Users/APIs: user(USER) and admin(USER, ADMIN); APIs are login, refresh, me, my orders, delete product.
// 3. RBAC | delete product | only ADMIN can access | implemented via requestMatchers + @PreAuthorize.
// 4. ABAC/ownership | my orders | real code checks ownerId == authenticated user | demo keeps only USER role check.

// requestMatchers vs @PreAuthorize flow:
// 1. requestMatchers run first in the Spring Security filter chain, before controller code.
//    They are good for broad URL rules like: /api/admin/** needs ADMIN, /api/auth/** is public.
// 2. @PreAuthorize runs later when the controller/service method is about to execute.
//    It is good for method/business rules like role checks, ownership checks, or permissions.
// 3. delete product uses @PreAuthorize at service level because the service method stays protected even
//    if a future controller, scheduler, or another code path calls it.
// 4. myOrders uses @PreAuthorize at controller level because this demo rule is tied directly to that endpoint.

// Example flows:
// - user(USER) -> DELETE /api/admin/products/1 -> blocked by requestMatchers('/api/admin/**').hasRole('ADMIN').
// - admin(USER, ADMIN) -> DELETE /api/admin/products/1 -> passes requestMatchers -> service @PreAuthorize('ADMIN') -> allowed.
// - unauthenticated -> GET /api/orders/my -> blocked because anyRequest().authenticated() needs a valid JWT.
// - user(USER) -> GET /api/orders/my -> passes authenticated route rule -> controller @PreAuthorize('USER') -> allowed.
// - admin(USER, ADMIN) -> GET /api/orders/my -> allowed in this demo because admin also has USER.

@EnableMethodSecurity // Enables @PreAuthorize method-level checks.
@SpringBootApplication
public class JwtAuthenticationRbacApplication {

    public static void main(String[] args) {
        SpringApplication.run(JwtAuthenticationRbacApplication.class, args);
    }
}

// 1. LOGIN
//    username + password
//    -> AuthController
//    -> AuthenticationManager
//    -> UserDetailsService loads user
//    -> PasswordEncoder verifies password
//    -> JWT generated.
// 2. CLIENT STORES TOKEN
//    Usually access token in memory/mobile secure storage.
//    Refresh token needs stronger protection: HttpOnly cookie or secure server-side tracking.
// 3. FUTURE REQUEST
//    Authorization: Bearer <JWT>
//    -> JwtAuthenticationFilter reads Authorization header
//    -> checks Bearer prefix
//    -> extracts JWT.
// 4. VALIDATION
//    Validate signature, expiration, token type, issuer/audience if used, and required claims.
//    Do not trust role/user data from an unverified token.
// 5. SECURITY CONTEXT
//    Extract user + roles
//    -> create Authentication
//    -> SecurityContext for current request
//    -> RBAC / @PreAuthorize
//    -> Controller.
