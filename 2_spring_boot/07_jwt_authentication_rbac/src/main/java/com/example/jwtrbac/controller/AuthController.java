package com.example.jwtrbac.controller;

import com.example.jwtrbac.dto.AuthResponse;
import com.example.jwtrbac.dto.LoginRequest;
import com.example.jwtrbac.dto.RefreshRequest;
import com.example.jwtrbac.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserDetailsService userDetailsService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    //------------------------------------------------------------------------

    // Login requires username and password.
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        // Step 1: authenticate credentials.
        // Flow: Controller -> AuthenticationManager -> UserDetailsService -> PasswordEncoder.
        // UserDetailsService decides where users are fetched from.
        // In this demo it is InMemoryUserDetailsManager; in real code it is usually DB-backed.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        // Step 2: after authentication succeeds, create tokens and return them to the client.
        return new AuthResponse(
                jwtService.createAccessToken(authentication),  // short-lived token used on normal API calls
                jwtService.createRefreshToken(authentication), // longer-lived token used only to get a new access token
                "Bearer"
        );
    }

    // Refresh flow mental model:
    // 1. Access token expires quickly, so the user should not type username/password again every few minutes.
    // 2. Client app calls /refresh with a refresh token; this is normally automatic, not a human action.
    // 3. Server validates signature + expiry + token type, loads the user, then creates a new access token.
    // 4. Production usually rotates refresh tokens: issue a new one, store only its hash, revoke the old one.
    // 5. Logout/revocation problem: a self-contained JWT cannot be "deleted" from the server because the server did not store it. Immediate invalidation needs state, for example denylist, token version, or stored refresh tokens.

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {

        // Step 1: validate refresh token signature, expiry, and typ=refresh.
        JwtService.JwtClaims claims = jwtService.validate(request.refreshToken(), "refresh");
        UserDetails userDetails = userDetailsService.loadUserByUsername(claims.subject());

        return new AuthResponse(
                jwtService.createAccessToken(userDetails),
                request.refreshToken(), // Demo keeps same refresh token; real code usually calls rotateRefreshToken(userDetails).
                "Bearer"
        );
    }
}

// This demo returns accessToken, refreshToken, and tokenType in the JSON response body to keep curl simple.
// Token transport choices:
// | Place | Common use | Pros | Cons |
// | JSON response body | Login/refresh response. | Easy for APIs and curl. | Browser JS can read it; must store carefully. |
// | Authorization header | Sending access token to APIs. | Not sent automatically by browser; lower CSRF risk. | Client must attach it on every request. |
// | HttpOnly cookie | Often used for refresh token. | JS cannot read it; helps against token theft by XSS. | Browser sends it automatically; consider CSRF protection. |

// ----------------------------------------------------------------

// HTTP request shape:
// | Example | Part | Meaning | Common values |
// | POST /api/users HTTP/1.1 | Request line | Method URI/Path HTTPVersion | GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS |
// | Host: example.com | Header | Target host | domain.com, api.domain.com |
// | Authorization: Bearer xyz123 | Header | Credentials/token | Bearer JWT, Basic credentials, API key |
// | User-Agent: Mozilla/5.0 | Header | Client identity | Browser, mobile app, curl, Postman |
// | Accept: application/json | Header | Desired response format | JSON, XML, HTML, */* |
// | Content-Type: application/json | Header | Request body format | JSON, XML, form data, binary |
// | Content-Length: 27 | Header | Body size in bytes | Numeric byte length |
// | {"name":"Aditya","age":26} | Body | Request data | JSON, XML, form data, binary, empty |

// HTTP response shape:
// | Example | Part | Meaning | Common values |
// | HTTP/1.1 201 Created | Status line | HTTPVersion StatusCode ReasonPhrase | 1xx, 2xx, 3xx, 4xx, 5xx |
// | Content-Type: application/json | Header | Response body format | JSON, XML, HTML, text |
// | Content-Length: 45 | Header | Body size in bytes | Numeric byte length |
// | Location: /api/users/101 | Header | Created resource / redirect target | URI or URL |
// | Set-Cookie: session_id=abc123 | Header | Cookie from server to browser | Session, refresh token, CSRF token, preferences |
// | Cache-Control: no-cache | Header | Cache behavior | no-cache, no-store, public, private, max-age=N |
// | {"id":101,"name":"Aditya","age":26} | Body | Response data | JSON, XML, HTML, text, binary, empty |