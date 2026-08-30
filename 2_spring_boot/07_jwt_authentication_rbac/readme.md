# 07 JWT Authentication + RBAC

## 1. Curl

### 1.1 Start The App

```bash
./run.sh
```

### 1.2 Login As USER

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "user",
    "password": "password"
  }'
```

### 1.3 Login As ADMIN

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "password": "password"
  }'
```

### 1.4 Call Authenticated Endpoint

```bash
TOKEN='<accessToken>'

curl http://localhost:8080/api/me \
  -H "Authorization: Bearer $TOKEN"
```

### 1.5 Call USER Endpoint

```bash
curl http://localhost:8080/api/orders/my \
  -H "Authorization: Bearer $TOKEN"
```

### 1.6 Call ADMIN Endpoint

```bash
curl -X DELETE http://localhost:8080/api/admin/products/101 \
  -H "Authorization: Bearer $TOKEN"
```

### 1.7 Refresh Access Token

```bash
curl -s -X POST http://localhost:8080/api/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{
    "refreshToken": "<refreshToken>"
  }'
```

## 2. Cheatsheet

### 2.1 Authentication Vs Authorization

| No. | Topic | Question | In This Module |
| --- | --- | --- | --- |
| 1 | Authentication | Who are you? | `/api/auth/login` verifies username/password. |
| 2 | Authorization | What can you do? | `ROLE_USER` and `ROLE_ADMIN` control endpoints/methods. |
| 3 | Principal | Which user is active? | `Authentication.getName()` returns `user` or `admin`. |
| 4 | Authority | Which permissions/roles are active? | `ROLE_USER`, `ROLE_ADMIN`. |

### 2.2 Session-Based Vs Token-Based

| No. | Style | Server Stores Login State? | Request Credential | Tradeoff |
| --- | --- | --- | --- | --- |
| 1 | Session | Yes | Cookie with session id | Easy revocation; server/session store required. |
| 2 | JWT access token | No for access token | `Authorization: Bearer <token>` | Scales statelessly; revocation is harder. |
| 3 | Refresh token | Usually stored/tracked in production | Refresh token body/cookie | Needed for long-lived login; must be protected/revoked. |

### 2.3 JWT Fundamentals

| No. | JWT Part | Meaning | Interview Note |
| --- | --- | --- | --- |
| 1 | Header | Algorithm and token type. | Example: `alg=HS256`, `typ=JWT`. |
| 2 | Payload | Claims like `sub`, `roles`, `iat`, `exp`. | Payload is Base64URL encoded, not encrypted. |
| 3 | Signature | HMAC/signature over header + payload. | Proves token was issued by trusted signer and was not modified. |
| 4 | Access token | Short-lived token for API calls. | Keep expiry short to limit theft impact. |
| 5 | Refresh token | Longer-lived token to obtain new access token. | Store/revoke in production. |

### 2.4 Signing Algorithms And Keys

| No. | Topic | Meaning | Production Note |
| --- | --- | --- | --- |
| 1 | Symmetric signing | Same secret signs and verifies tokens. | `HS256` is common for simple/internal systems. |
| 2 | Asymmetric signing | Private key signs; public key verifies. | `RS256` is useful when many services verify tokens but should not have the private signing key. |
| 3 | Signing key | Credential that proves token authenticity. | Protect with environment variables, secret manager, KMS, or vault. |
| 4 | Key rotation | Replacing signing keys safely over time. | Use key ids (`kid`) and support old + new keys during rollout. |
| 5 | Algorithm allowlist | Server accepts only expected algorithms. | Never trust token header blindly; reject unexpected `alg`. |

### 2.5 Spring Security Architecture

| No. | Component | Job |
| --- | --- | --- |
| 1 | `SecurityFilterChain` | Declares HTTP security rules and installs JWT filter. |
| 2 | `JwtAuthenticationFilter` | Reads Bearer token, validates it, sets `SecurityContext`. |
| 3 | `AuthenticationManager` | Verifies username/password during login. |
| 4 | `UserDetailsService` | Loads user and roles. |
| 5 | `PasswordEncoder` | Verifies BCrypt password hash. |
| 6 | `SecurityContext` | Holds authenticated user for the current request thread. |

### 2.6 JWT Request Flow

| No. | Step | Code |
| --- | --- | --- |
| 1 | Request arrives with Bearer token | `Authorization: Bearer <accessToken>` |
| 2 | Filter extracts token | `JwtAuthenticationFilter` |
| 3 | Token is validated | `JwtService.validate(token, "access")` |
| 4 | User is loaded | `UserDetailsService.loadUserByUsername(...)` |
| 5 | Authentication is created | `UsernamePasswordAuthenticationToken` |
| 6 | Security context is set | `SecurityContextHolder.getContext().setAuthentication(...)` |
| 7 | Controller/method authorization runs | `hasRole(...)`, `@PreAuthorize(...)` |

### 2.7 Password Security

| No. | Topic | Production Rule |
| --- | --- | --- |
| 1 | Plain password | Never store directly. |
| 2 | Hashing | One-way transformation used for password storage. |
| 3 | Encryption | Two-way transformation; not the normal password storage approach. |
| 4 | BCrypt | Slow salted hash; good default for Spring apps. |
| 5 | Argon2 | Another strong password-hashing option when supported by your stack. |
| 6 | Login | Hash candidate password and compare through `PasswordEncoder.matches(...)`. |

### 2.8 RBAC

| No. | Role | Allowed Examples |
| --- | --- | --- |
| 1 | `USER` | View own profile, view own orders. |
| 2 | `ADMIN` | Everything `USER` can do plus manage products/users. |
| 3 | Role naming | Spring `hasRole('ADMIN')` checks for authority `ROLE_ADMIN`. |
| 4 | This module | `admin` has both `ROLE_USER` and `ROLE_ADMIN`. |

### 2.9 Method-Level Authorization

| No. | Code | Meaning |
| --- | --- | --- |
| 1 | `@EnableMethodSecurity` | Enables `@PreAuthorize`. |
| 2 | `@PreAuthorize("hasRole('USER')")` | Role-based access; checks `ROLE_USER`. |
| 3 | `@PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")` | At least one role must match. |
| 4 | `@PreAuthorize("hasAuthority('ORDER_READ')")` | Permission-style access; exact authority string. |
| 5 | `@PreAuthorize("hasAnyAuthority('ORDER_READ', 'ORDER_WRITE')")` | At least one authority must match. |
| 6 | `@PreAuthorize("hasRole('ADMIN') and hasAuthority('PRODUCT_DELETE')")` | Both conditions must pass. |
| 7 | `@PreAuthorize("hasRole('ADMIN') or principal.username == #username")` | Role OR current-user ownership check. |
| 8 | `@PreAuthorize("isAuthenticated()")` | Any logged-in user. |
| 9 | `@PreAuthorize("isAnonymous()")` | Only users without login/authentication. |
| 10 | `@PreAuthorize("permitAll")` / `@PreAuthorize("denyAll")` | Always allow / always block; usually cleaner at `requestMatchers` level. |
| 11 | `@PreAuthorize("@orderSecurity.canView(#id, authentication)")` | Custom bean check for ownership/business rules. |
| 12 | Service method protection | Protects method even if another controller calls it later. |

### 2.10 Access Token + Refresh Token

| No. | Topic | Production Habit |
| --- | --- | --- |
| 1 | Access token | Short-lived; sent on API calls. |
| 2 | Refresh token | Longer-lived; used only to get a new access token. |
| 3 | Expiration | Always validate `exp`. |
| 4 | Refresh storage | Store hashed refresh tokens server-side in production. |
| 5 | Logout/revocation | Stateless access tokens are hard to revoke before expiry. |

### 2.11 JWT Validation Checklist

| No. | Check | Why |
| --- | --- | --- |
| 1 | Signature | Confirms token was signed by trusted key and not modified. |
| 2 | Expiration `exp` | Rejects old tokens. |
| 3 | Issuer `iss` | Confirms token came from expected auth service. |
| 4 | Audience `aud` | Confirms token was meant for this API/service. |
| 5 | Token type | Separates `access` token use from `refresh` token use. |
| 6 | Algorithm | Prevents accepting unexpected or weak signing algorithms. |
| 7 | User status | Optional DB/cache check for disabled users or token version changes. |

### 2.12 Production Best Practices

| No. | Practice | Why |
| --- | --- | --- |
| 1 | Always use HTTPS. | Tokens are bearer credentials; whoever has one can use it. |
| 2 | Protect the JWT signing key. | Store secrets/keys in env, secret manager, vault, or KMS. |
| 3 | Use strong signing algorithms and key management. | Choose deliberately: `HS256` for simple/internal systems, `RS256` when many services verify tokens without holding the private key. |
| 4 | Do not put sensitive information in JWT claims. | JWT payload is readable unless separately encrypted. |
| 5 | Keep access tokens short-lived. | Limits damage from token theft and stale permissions. |
| 6 | Secure refresh tokens. | Store server-side as hashes, rotate on use, and revoke on logout/risk. |
| 7 | Hash passwords with BCrypt, Argon2, or another password hashing scheme. | Passwords need slow salted one-way hashing, not encryption/plaintext. |
| 8 | Enforce authorization server-side. | UI hiding is not security; API must check roles/authorities like `ROLE_ADMIN`. |
| 9 | Check resource ownership. | `ROLE_USER` may still only view their own orders, not every user's orders. |
| 10 | Validate more than the signature. | Check expiry, issuer, audience, token type, algorithm, and user status when needed. |
| 11 | Rate-limit authentication endpoints. | Login and refresh endpoints are brute-force and credential-stuffing targets. |

### 2.13 Production Concerns

| No. | Concern | Habit |
| --- | --- | --- |
| 1 | Secret/key management | Use env/secret manager; rotate keys. |
| 2 | HTTPS | Required; tokens are bearer credentials. |
| 3 | JWT theft | Short expiry, secure storage, monitoring, revocation strategy. |
| 4 | CORS | Controls which browser origins can read API responses. |
| 5 | CSRF | Mainly a cookie/browser concern; bearer tokens in headers reduce but do not erase all browser risk. |
| 6 | Least privilege | Put only needed roles/scopes in token. |
| 7 | Token content | Do not put secrets or sensitive PII in JWT payload. |

## 3. Interview QA

| No. | Scenario / Tricky Question | Strong SDE2 Answer |
| --- | --- | --- |
| 1 | Authentication vs authorization? | Authentication identifies the user; authorization decides what that user can access. |
| 2 | Why is JWT useful? | It lets APIs authenticate requests without server-side access-token session lookup, useful for stateless services and distributed systems. |
| 3 | Is JWT encrypted? | Not by default. Standard JWT payload is Base64URL encoded and signed; anyone with the token can read claims. |
| 4 | What does the JWT signature prove? | The token was signed by the trusted key and header/payload were not modified. |
| 5 | Why short-lived access tokens? | Reduces impact if stolen and limits stale permissions. |
| 6 | Why use refresh tokens? | Keeps access tokens short-lived while allowing users to obtain new access tokens without logging in every few minutes. |
| 7 | Why is logout hard with JWT? | A stateless access token remains valid until expiry unless you add server-side revocation/denylist/token versioning. |
| 8 | Where should roles be enforced? | At HTTP route rules and/or method level. Method-level checks protect service methods from future entry points. |
| 9 | Why BCrypt? | Passwords should be stored as slow salted hashes, not plaintext or reversible encryption. |
| 10 | What does `SecurityContext` hold? | The authenticated principal and authorities for the current request. |
| 11 | Why can `hasRole('ADMIN')` fail when token says `ADMIN`? | Spring roles are authorities prefixed with `ROLE_`; `hasRole('ADMIN')` checks `ROLE_ADMIN`. |
| 12 | CORS vs CSRF? | CORS controls whether browser JavaScript can read cross-origin responses. CSRF tricks an authenticated browser into sending a request. They solve different problems. |
| 13 | What belongs in JWT claims? | Stable identity and authorization facts needed by the API, with no secrets or sensitive PII. |
| 14 | What is the production concern with HS256 secret? | Every verifier with the secret can also sign tokens. Protect the secret and consider asymmetric keys for larger systems. |
| 15 | What should happen if token validation fails? | Do not set `SecurityContext`; let Spring Security return unauthorized/forbidden based on endpoint rules. |

## 4. DB Design

### 4.1 Core Identity And RBAC Tables

| No. | Table | Main Columns | Purpose |
| --- | --- | --- | --- |
| 1 | `users` | `id`, `username`, `email`, `password_hash`, `status`, `created_at`, `updated_at`, `last_login_at` | Stores user identity and account state. |
| 2 | `roles` | `id`, `name`, `description`, `created_at` | Stores roles like `USER`, `ADMIN`, `MANAGER`. |
| 3 | `permissions` | `id`, `name`, `description`, `created_at` | Stores fine-grained permissions like `ORDER_READ`, `ORDER_WRITE`. |
| 4 | `user_roles` | `user_id`, `role_id` | Maps users to roles. |
| 5 | `role_permissions` | `role_id`, `permission_id` | Maps roles to permissions. |

### 4.2 Token And Session Tables

| No. | Table | Main Columns | Purpose |
| --- | --- | --- | --- |
| 1 | `refresh_tokens` | `id`, `user_id`, `token_hash`, `expires_at`, `revoked_at`, `created_at`, `replaced_by_token_id`, `device_id`, `ip_address` | Stores refresh-token state for rotation, expiry, and revocation. |
| 2 | `access_token_blacklist` | `jti`, `user_id`, `expires_at`, `revoked_at` | Tracks revoked access tokens only when immediate JWT revocation is required. |
| 3 | `user_sessions` | `id`, `user_id`, `refresh_token_id`, `device_id`, `ip_address`, `user_agent`, `created_at`, `last_seen_at`, `expires_at`, `revoked_at` | Tracks login sessions/devices separately from individual access tokens. |

### 4.3 Operational Security Tables

| No. | Table | Main Columns | Purpose |
| --- | --- | --- | --- |
| 1 | `login_attempts` | `id`, `identifier`, `ip_address`, `success`, `attempted_at` | Supports brute-force detection and login throttling. |
| 2 | `rate_limit_policies` | `id`, `name`, `scope`, `limit`, `window_seconds`, `created_at`, `updated_at` | Defines rate-limit rules for users, IPs, API keys, or endpoints. |
| 3 | `rate_limit_counters` | `id`, `policy_id`, `key`, `window_start`, `request_count`, `expires_at` | Stores request counters for rate limiting. |
| 4 | `audit_logs` | `id`, `user_id`, `action`, `resource_type`, `resource_id`, `ip_address`, `user_agent`, `metadata`, `created_at` | Records security-sensitive actions. |

### 4.4 Extra Auth Tables

| No. | Table | Main Columns | Purpose |
| --- | --- | --- | --- |
| 1 | `password_reset_tokens` | `id`, `user_id`, `token_hash`, `expires_at`, `used_at`, `created_at` | Stores password-reset token state. |
| 2 | `email_verification_tokens` | `id`, `user_id`, `token_hash`, `expires_at`, `verified_at`, `created_at` | Stores email-verification token state. |
| 3 | `api_keys` | `id`, `user_id`, `key_hash`, `name`, `status`, `expires_at`, `last_used_at`, `created_at`, `revoked_at` | Stores API credentials for integrations/programmatic clients. |
| 4 | `api_key_permissions` | `api_key_id`, `permission_id` | Maps API keys to allowed permissions. |

### 4.5 Minimal Starting Point

| No. | Need | Start With |
| --- | --- | --- |
| 1 | Login + RBAC | `users`, `roles`, `user_roles`. |
| 2 | Fine-grained authorization | Add `permissions`, `role_permissions`. |
| 3 | Refresh-token rotation/logout | Add `refresh_tokens` and optionally `user_sessions`. |
| 4 | Immediate access-token revocation | Add `access_token_blacklist`, but only if short access-token expiry is not enough. |
| 5 | Production audit/security | Add `login_attempts`, `rate_limit_*`, and `audit_logs`. |

