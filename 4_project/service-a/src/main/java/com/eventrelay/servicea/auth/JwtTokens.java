package com.eventrelay.servicea.auth;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class JwtTokens {
    private final NimbusJwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    public JwtTokens(@Value("${app.auth.jwt-key:}") String configured,
                     @Value("${app.auth.key-file:.local/auth.key}") String filename) throws java.io.IOException {
        String encoded = configured;
        if (encoded.isBlank()) {
            Path path = Path.of(filename).toAbsolutePath();
            Files.createDirectories(path.getParent(), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            if (!Files.exists(path)) {
                Path temp = Files.createTempFile(path.getParent(), ".auth-", ".tmp", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                try {
                    byte[] value = new byte[32]; new SecureRandom().nextBytes(value);
                    Files.writeString(temp, Base64.getEncoder().encodeToString(value));
                    try { Files.move(temp, path); } catch (FileAlreadyExistsException ignored) { }
                } finally { Files.deleteIfExists(temp); }
            }
            encoded = Files.readString(path).strip();
        }
        byte[] bytes = Base64.getDecoder().decode(encoded);
        if (bytes.length != 32) throw new IllegalArgumentException("AUTH_JWT_KEY must contain 32 Base64-encoded bytes");
        var key = new SecretKeySpec(bytes, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("event-relay"));
    }
    public String issue(SessionUser user) {
        var claims = JwtClaimsSet.builder().issuer("event-relay").subject(user.id().toString())
                .id(user.sessionId().toString()).issuedAt(Instant.now()).expiresAt(user.expiresAt()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
    public Jwt decode(String value) { return decoder.decode(value); }
}
