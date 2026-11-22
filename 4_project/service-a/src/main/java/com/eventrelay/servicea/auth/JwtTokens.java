package com.eventrelay.servicea.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

// S2: jwt constructor + create + validate incoming + decode incoming
@Component
public class JwtTokens {
    private final NimbusJwtEncoder encoder;
    private final NimbusJwtDecoder decoder;

    //constructor
    public JwtTokens(@Value("${app.auth.jwt-key:}") String configured,
            @Value("${app.auth.key-file:.local/auth.key}") String filename) throws IOException {

        //to create a jwt, we need to sign it which requires key -- read from file or create it (issue - other instance needs to point to same path)
        String encoded = configured;
        if (encoded.isBlank()) {
            Path path = Path.of(filename).toAbsolutePath();
            Files.createDirectories(path.getParent(),
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            if (!Files.exists(path)) {
                Path temp = Files.createTempFile(path.getParent(), ".auth-", ".tmp",
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                try {
                    byte[] value = new byte[32];
                    new SecureRandom().nextBytes(value);
                    Files.writeString(temp, Base64.getEncoder().encodeToString(value));
                    try {
                        Files.move(temp, path);
                    } catch (FileAlreadyExistsException ignored) {
                    }
                } finally {
                    Files.deleteIfExists(temp);
                }
            }
            encoded = Files.readString(path).strip();
        }
        byte[] bytes = Base64.getDecoder().decode(encoded);
        if (bytes.length != 32)
            throw new IllegalArgumentException("AUTH_JWT_KEY must contain 32 Base64-encoded bytes");
        var jwt_signing_key = new SecretKeySpec(bytes, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(jwt_signing_key));
        decoder = NimbusJwtDecoder.withSecretKey(jwt_signing_key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO), new JwtIssuerValidator("event-relay"), this::validateClaims));
    }

    //called from JwtCookies.issue and it create jwt
    public String issue(JwtUser authenticated_user) {
        var claims = JwtClaimsSet.builder().issuer("event-relay").subject(authenticated_user.id().toString())
                .id(authenticated_user.tokenId().toString()).issuedAt(Instant.now()).expiresAt(authenticated_user.expiresAt())
                .audience(List.of("event-relay-dashboard")).claim("role", authenticated_user.role()).claim("token_use", "access")
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    //authenticate fn calls it and it decodes recieved jwt
    public Jwt decode(String value) {
        return decoder.decode(value);
    }

    //called from JwtAuthenticationFilter.doFilterInternal and calls decode and returns User(id, role, tokenid, expires at)
    public JwtUser authenticate(String value) {
        var jwt = decode(value);
        return new JwtUser(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"),
                UUID.fromString(jwt.getId()), jwt.getExpiresAt());
    }

    //used in constructor and validates structure
    private OAuth2TokenValidatorResult validateClaims(Jwt jwt) {
        try {
            UUID.fromString(jwt.getSubject());
            UUID.fromString(jwt.getId());
            Object role = jwt.getClaims().get("role");
            if (!(role instanceof String) || !Set.of("USER", "ADMIN").contains(role)
                    || !"access".equals(jwt.getClaims().get("token_use"))
                    || !jwt.getAudience().contains("event-relay-dashboard")
                    || jwt.getIssuedAt() == null || jwt.getExpiresAt() == null
                    || !jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                    || jwt.getIssuedAt().isAfter(Instant.now().plusSeconds(30)))
                throw new IllegalArgumentException();
            return OAuth2TokenValidatorResult.success();
        } catch (IllegalArgumentException | NullPointerException invalid) {
            return OAuth2TokenValidatorResult
                    .failure(new OAuth2Error("invalid_token", "Required access-token claims are invalid", null));
        }
    }
}
