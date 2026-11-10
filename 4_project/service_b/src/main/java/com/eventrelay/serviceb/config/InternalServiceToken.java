package com.eventrelay.serviceb.config;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Shared service credential, separate from customer API keys and dashboard cookies. */
@Component
public class InternalServiceToken {
    private final String value;
    public InternalServiceToken(@Value("${app.internal.token:}") String configured,
            @Value("${app.internal.key-file:../service-a/.local/internal.key}") String filename) throws java.io.IOException {
        if(configured.isBlank()) {
            Path path=Path.of(filename).toAbsolutePath();
            Files.createDirectories(path.getParent(),PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            if(!Files.exists(path)) {
                Path temp=Files.createTempFile(path.getParent(),".internal-",".tmp",PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                try {
                    byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
                    Files.writeString(temp,Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
                    try {Files.createLink(path,temp);}catch(FileAlreadyExistsException ignored) {}
                }finally {Files.deleteIfExists(temp);}
            }
            configured=Files.readString(path).strip();
        }
        if(configured.length()<32||configured.length()>256||!configured.matches("[A-Za-z0-9_=-]+")) throw new IllegalArgumentException("Invalid internal service credential");
        value=configured;
    }
    public String value() {return value;}
    public boolean matches(String authorization) {
        return authorization!=null&&MessageDigest.isEqual(("Bearer "+value).getBytes(java.nio.charset.StandardCharsets.UTF_8),authorization.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
