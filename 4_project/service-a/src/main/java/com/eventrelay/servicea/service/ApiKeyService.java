package com.eventrelay.servicea.service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.eventrelay.servicea.dto.Responses;
import com.eventrelay.servicea.entity.ApiKey;
import com.eventrelay.servicea.repository.*;

@Service
@Transactional(readOnly = true)
public class ApiKeyService {
    private final ApiKeyRepository keys;
    private final ResourceAccess access;
    private final SecureRandom random = new SecureRandom();

    public ApiKeyService(ApiKeyRepository keys, ResourceAccess access) {
        this.keys = keys;
        this.access = access;
    }

    @Transactional
    public Responses.IssuedApiKey create(UUID applicationId, String name) {
        access.lockedApplication(applicationId);
        return issue(applicationId, name.strip());
    }

    public Responses.PageResult<Responses.ApiKeyDetails> list(UUID applicationId, Pageable page) {
        requireApplication(applicationId);
        return Responses.PageResult.from(keys.findByApplicationId(applicationId, page).map(Responses.ApiKeyDetails::from));
    }

    public Responses.ApiKeyDetails get(UUID applicationId, UUID keyId) {
        requireApplication(applicationId);
        return Responses.ApiKeyDetails.from(keys.findByIdAndApplicationId(keyId, applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found")));
    }

    @Transactional
    public Responses.IssuedApiKey rotate(UUID applicationId, UUID keyId) {
        access.lockedApplication(applicationId);
        var oldKey = lockedKey(applicationId, keyId);
        if (oldKey.getRevokedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Revoked API keys cannot be rotated");
        }
        var replacement = issue(applicationId, oldKey.getName());
        oldKey.replaceWith(replacement.key().id());
        return replacement;
    }

    @Transactional
    public void revoke(UUID applicationId, UUID keyId) {
        access.lockedApplication(applicationId);
        lockedKey(applicationId, keyId).revoke();
    }

    // Authentication boundary for ingestion and event-status reads.
    public UUID authenticate(String secret) {
        if (secret == null || !secret.matches("er_[A-Za-z0-9_-]{43}")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key");
        }
        UUID appId = keys.findByKeyHashAndRevokedAtIsNull(hash(secret)).map(ApiKey::getApplicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key"));
        try { access.application(appId); } catch (ResponseStatusException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key");
        }
        return appId;
    }

    private Responses.IssuedApiKey issue(UUID applicationId, String name) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String secret = "er_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var key = new ApiKey(applicationId, name, hash(secret), secret.substring(0, 11));
        return new Responses.IssuedApiKey(Responses.ApiKeyDetails.from(keys.saveAndFlush(key)), secret);
    }

    private static String hash(String secret) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private ApiKey lockedKey(UUID applicationId, UUID keyId) {
        return keys.findForUpdate(keyId, applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found"));
    }

    private void requireApplication(UUID applicationId) {
        access.application(applicationId);
    }
}
