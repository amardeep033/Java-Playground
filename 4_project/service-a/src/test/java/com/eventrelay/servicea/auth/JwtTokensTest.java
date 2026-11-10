package com.eventrelay.servicea.auth;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.oauth2.jwt.JwtException;
import static org.junit.jupiter.api.Assertions.*;

class JwtTokensTest {
    @TempDir Path directory;
    private JwtUser user(Instant expiresAt) {
        return new JwtUser(UUID.randomUUID(), "USER", UUID.randomUUID(), expiresAt);
    }
    private JwtTokens tokens(String file) throws Exception {
        return new JwtTokens("", directory.resolve(file).toString());
    }
    @Test void persistsTheSigningKeyAcrossRestartsWithoutPersistingTheToken() throws Exception {
        var first = tokens("auth.key");
        var user = user(Instant.now().plusSeconds(3600));
        String token = first.issue(user);
        var decoded = tokens("auth.key").decode(token);
        assertEquals(user.id().toString(), decoded.getSubject());
        assertEquals(user.tokenId().toString(), decoded.getId());
        assertEquals("event-relay", decoded.getClaimAsString("iss"));
        assertFalse(decoded.hasClaim("password"));
        assertEquals("USER",decoded.getClaimAsString("role"));
        assertEquals("USER",tokens("auth.key").authenticate(token).role());
        assertTrue(decoded.getAudience().contains("event-relay-dashboard"));
    }
    @Test void rejectsModifiedPayloadWithOriginalSignature() throws Exception {
        var tokens = tokens("auth.key");
        String[] parts = tokens.issue(user(Instant.now().plusSeconds(3600))).split("\\.");
        parts[1] = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"admin\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThrows(JwtException.class, () -> tokens.decode(String.join(".", parts)));
    }
    @Test void rejectsExpiredJwtEvenWithValidSignature() throws Exception {
        var tokens = tokens("auth.key");
        var claims = new com.nimbusds.jwt.JWTClaimsSet.Builder().issuer("event-relay").audience("event-relay-dashboard").claim("role","USER").claim("token_use","access")
                .subject(UUID.randomUUID().toString()).jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(Instant.now().minusSeconds(600)))
                .expirationTime(Date.from(Instant.now().minusSeconds(300))).build();
        var signed = new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256), claims);
        byte[] key = Base64.getDecoder().decode(java.nio.file.Files.readString(directory.resolve("auth.key")));
        signed.sign(new com.nimbusds.jose.crypto.MACSigner(key));
        String token = signed.serialize();
        assertThrows(JwtException.class, () -> tokens.decode(token));
    }
    @Test void rejectsAnotherServersSigningKey() throws Exception {
        String token = tokens("first.key").issue(user(Instant.now().plusSeconds(3600)));
        var other = tokens("other.key");
        assertThrows(JwtException.class, () -> other.decode(token));
    }
    @Test void refusesUndersizedConfiguredKey() {
        assertThrows(IllegalArgumentException.class, () -> new JwtTokens(Base64.getEncoder().encodeToString(new byte[16]), "unused"));
    }
    private String signedToken(String role,String audience,String issuer,boolean includeRole) throws Exception {
        tokens("auth.key");
        var claims=new com.nimbusds.jwt.JWTClaimsSet.Builder().issuer(issuer).audience(audience)
                .subject(UUID.randomUUID().toString()).jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(Instant.now())).expirationTime(Date.from(Instant.now().plusSeconds(600)))
                .claim("token_use","access");
        if(includeRole) claims.claim("role",role);
        var signed=new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256),claims.build());
        signed.sign(new com.nimbusds.jose.crypto.MACSigner(Base64.getDecoder().decode(java.nio.file.Files.readString(directory.resolve("auth.key")))));
        return signed.serialize();
    }
    @Test void rejectsLegacyTokensWithoutSignedRole() throws Exception {
        var tokens=tokens("auth.key");var token=signedToken("USER","event-relay-dashboard","event-relay",false);
        assertThrows(JwtException.class,()->tokens.authenticate(token));
    }
    @Test void rejectsUnknownRoleWrongAudienceAndWrongIssuer() throws Exception {
        var tokens=tokens("auth.key");
        for(var token:List.of(signedToken("SUPERADMIN","event-relay-dashboard","event-relay",true),
                signedToken("USER","another-service","event-relay",true),signedToken("APP_ADMIN","event-relay-dashboard","another-issuer",true))) {
            assertThrows(JwtException.class,()->tokens.authenticate(token));
        }
    }
}
