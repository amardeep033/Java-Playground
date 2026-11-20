package com.eventrelay.servicea.apikey;

import com.eventrelay.servicea.apikey.dto.ApiKeyDetails;
import com.eventrelay.servicea.apikey.dto.IssuedApiKey;
import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.common.dto.PageResult;
import com.eventrelay.servicea.common.dto.UpdateName;
import com.eventrelay.servicea.common.search.ListSearch;
import com.eventrelay.servicea.workspace.AuditLogs;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ApiKeyService {
    private final ApiKeyRepository keys;
    private final ResourceAccess access;
    private final SecureRandom random = new SecureRandom();

    private final AuditLogs audit;

    public ApiKeyService(ApiKeyRepository keys, ResourceAccess access, AuditLogs audit) {
        this.audit = audit;
        this.keys = keys;
        this.access = access;
    }

    @Transactional
    public IssuedApiKey create(UUID applicationId, String name) {
        access.lockedApplication(applicationId);
        return issue(applicationId, name.strip());
    }

    public PageResult<ApiKeyDetails> list(UUID appId, String q, String status, Pageable page) {
        requireApplication(appId);
        var spec = ListSearch.<ApiKey>scoped("applicationId", appId, q, "name", "keyPrefix");
        var selected = ListSearch.choices(status, Set.of("ACTIVE", "REVOKED"));
        if (selected.size() == 1)
            spec = spec.and((root, query, cb) -> selected.getFirst().equals("ACTIVE") ? cb.isNull(root.get("revokedAt"))
                    : cb.isNotNull(root.get("revokedAt")));
        return PageResult.from(keys.findAll(spec, page).map(ApiKeyDetails::from));
    }

    @Transactional
    public ApiKeyDetails update(UUID appId, UUID keyId, UpdateName request) {
        access.lockedApplication(appId);
        var key = lockedKey(appId, keyId);
        audit.changed(appId, "api_keys", keyId, "name", key.getName(), request.name().strip());
        key.updateName(request.name().strip());
        return ApiKeyDetails.from(key);
    }

    public ApiKeyDetails get(UUID applicationId, UUID keyId) {
        requireApplication(applicationId);
        return ApiKeyDetails.from(keys.findByIdAndApplicationId(keyId, applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found")));
    }

    @Transactional
    public IssuedApiKey rotate(UUID applicationId, UUID keyId) {
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
        // Only unrevoked key hashes authenticate. Revocation takes effect on the
        // next API-key request; it neither invalidates dashboard JWTs nor cancels
        // queued deliveries.
        UUID appId = keys.findByKeyHashAndRevokedAtIsNull(hash(secret)).map(ApiKey::getApplicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key"));
        try {
            access.application(appId);
        } catch (ResponseStatusException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key");
        }
        return appId;
    }

    private IssuedApiKey issue(UUID applicationId, String name) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String secret = "er_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var key = new ApiKey(applicationId, name, hash(secret), secret.substring(0, 11));
        return new IssuedApiKey(ApiKeyDetails.from(keys.saveAndFlush(key)), secret);
    }

    private static String hash(String secret) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)));
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
