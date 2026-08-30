package com.example.jwtrbac.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;


@Service
public class JwtService {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long accessTokenSeconds;
    private final long refreshTokenSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            // Demo property only. Real signing keys belong in env/secret manager/KMS, not hardcoded or stored in normal DB rows.
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-seconds}") long accessTokenSeconds,
            @Value("${app.jwt.refresh-token-seconds}") long refreshTokenSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSeconds = accessTokenSeconds;
        this.refreshTokenSeconds = refreshTokenSeconds;
    }

    //-----------------------------------------------------------------------------------------

    // Access token created after the first successful login; sent on normal API calls.
    public String createAccessToken(Authentication authentication) {
        return createToken(authentication.getName(), roles(authentication), "access", accessTokenSeconds);
    }

    // Refresh token created after first login; used to obtain a new access token without re-entering password.
    public String createRefreshToken(Authentication authentication) {
        // Demo shortcut: this refresh token is also a JWT.
        // Production preference: use a long random opaque refresh token, store only its hash in DB,
        // rotate it on every refresh, and revoke it on logout/risk. Token theft is still serious.
        return createToken(authentication.getName(), roles(authentication), "refresh", refreshTokenSeconds);
    }

    // Access token created during refresh flow; user does not re-enter username/password.
    public String createAccessToken(UserDetails userDetails) {
        return createToken(userDetails.getUsername(), roles(userDetails), "access", accessTokenSeconds);
    }

    // JWT shape: Authorization: Bearer xxxxx.yyyyy.zzzzz
    // 1. Header | Metadata about token/signing. | alg=HS256, typ=JWT. |
    // 2. Payload | Claims about subject and token lifetime. | sub, iat, exp, iss, aud, roles. |
    // 3. Signature | Integrity proof over header + payload. | sign(base64url(header) + "." + base64url(payload), secret/private-key). |
    
    // sub | Subject/user identity. | user, admin, 42, user-123. |
    // iat | Issued-at timestamp. | 1735689600. |
    // exp | Expiration timestamp. | 1735690500. |
    // iss | Issuer/auth server. | https://auth.example.com. |
    // aud | Audience/API intended to accept this token. | checkout-api, product-service. |
    // typ | Token purpose/type. | access, refresh. |
    // roles | Role/authority claims used by API. | [ROLE_USER], [ROLE_USER, ROLE_ADMIN]. |
    // custom | App-specific claims; do not put secrets here. | userId=42, tenantId=acme. |

    private String createToken(String subject, List<String> roles, String type, long ttlSeconds) {
        try {
            Map<String, Object> header = Map.of(
                    "alg", "HS256",
                    "typ", "JWT"
            );
            Map<String, Object> payload = Map.of(
                    "sub", subject,
                    "roles", roles,
                    "typ", type,
                    "iat", Instant.now().getEpochSecond(),
                    "exp", Instant.now().plusSeconds(ttlSeconds).getEpochSecond()
            );

            // JWT payload is encoded, not encrypted. Anyone holding the token can read it.
            // Do not put secrets, passwords, or sensitive PII in claims.
            String encodedHeader = encodeJson(header);
            String encodedPayload = encodeJson(payload);
            String signedContent = encodedHeader + "." + encodedPayload;

            // If someone edits payload, for example changing roles from USER to ADMIN,
            // the expected signature no longer matches, so validation rejects the token.
            return signedContent + "." + sign(signedContent);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create JWT", exception);
        }
    }

    //-----------------------------------------------------------------------------------------

    public JwtClaims validate(String token, String expectedType) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("JWT must have header, payload, and signature");
            }

            String signedContent = parts[0] + "." + parts[1];
            String expectedSignature = sign(signedContent);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }

            Map<String, Object> payload = readJson(parts[1]);
            String subject = requiredString(payload, "sub");
            String type = requiredString(payload, "typ");
            long expiresAt = ((Number) payload.get("exp")).longValue();

            if (!expectedType.equals(type)) {
                throw new IllegalArgumentException("Unexpected JWT type");
            }

            if (Instant.now().getEpochSecond() >= expiresAt) {
                throw new IllegalArgumentException("JWT expired");
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) payload.getOrDefault("roles", List.of());
            return new JwtClaims(subject, roles, type, expiresAt);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid JWT", exception);
        }
    }

    //--------------------------------UTILITY HELPER FN----------------------------------

    private List<String> roles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    private List<String> roles(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private Map<String, Object> readJson(String encoded) throws Exception {
        byte[] json = BASE64_URL_DECODER.decode(encoded);
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    // | HMAC / HS256 | Same secret signs and verifies. | Simple services; protect shared secret carefully. |
    // | RSA / RS256 | Private key signs, public key verifies. | Many services can verify without having signing power. |
    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return BASE64_URL_ENCODER.encodeToString(signature);
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);

        if (leftBytes.length != rightBytes.length) {
            return false;
        }

        int result = 0;
        for (int index = 0; index < leftBytes.length; index++) {
            result |= leftBytes[index] ^ rightBytes[index];
        }
        return result == 0;
    }

    private String requiredString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (!(value instanceof String stringValue)) {
            throw new IllegalArgumentException("Missing JWT claim: " + key);
        }
        return stringValue;
    }

    public record JwtClaims(String subject, List<String> roles, String type, long expiresAt) {
    }
}

// Crypto
// ├── Reversible encryption: original data can come back after decrypting.
// │   ├── Symmetric encryption: same secret key encrypts and decrypts.
// │   │   └── Example: AES; useful for protecting stored/transmitted data. Not used by this JWT demo.
// │   └── Asymmetric encryption: public key encrypts, private key decrypts.
// │       └── Example: RSA encryption; different from JWT signing.
// │
// ├── One-way hashing: original data should not come back.
// │   ├── Fast hash
// │   │   └── SHA-256: good for fingerprints/integrity, too fast for password storage by itself.
// │   └── Password hash
// │       ├── BCrypt
// │       └── Argon2
// │           Use for storing passwords; slow + salted; not used for signing JWTs.
// │
// └── Integrity / signing: prove data was not modified and came from a trusted signer.
//     ├── Symmetric MAC / HMAC: same secret creates and verifies the tag.
//     │   ├── HMAC-SHA256: generic crypto name.
//     │   ├── HmacSHA256: Java algorithm name used by Mac.getInstance("HmacSHA256").
//     │   └── HS256: JWT algorithm name for HMAC-SHA256; same secret signs and verifies.
//     └── Asymmetric digital signature: private key signs, public key verifies.
//         ├── RSA-SHA256 / RS256: JWT signing with RSA signatures.
//         └── Useful when many services verify tokens but should not have signing power.
