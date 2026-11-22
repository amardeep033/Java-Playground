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
    public IssuedApiKey create(UUID app_id, String name) {
        access.lockedApplication(app_id);
        return issue(app_id, name.strip());
    }

    public PageResult<ApiKeyDetails> list(UUID app_id, String q, String status, Pageable page) {
        requireApplication(app_id);
        var spec = ListSearch.<ApiKey>scoped("applicationId", app_id, q, "name", "keyPrefix");
        var selected = ListSearch.choices(status, Set.of("ACTIVE", "REVOKED"));
        if (selected.size() == 1)
            spec = spec.and((root, query, cb) -> selected.getFirst().equals("ACTIVE") ? cb.isNull(root.get("revokedAt"))
                    : cb.isNotNull(root.get("revokedAt")));
        return PageResult.from(keys.findAll(spec, page).map(ApiKeyDetails::from));
    }

    @Transactional
    public ApiKeyDetails update(UUID app_id, UUID api_key_id, UpdateName request) {
        access.lockedApplication(app_id);
        var api_key_record = lockedKey(app_id, api_key_id);
        audit.changed(app_id, "api_keys", api_key_id, "name", api_key_record.getName(), request.name().strip());
        api_key_record.updateName(request.name().strip());
        return ApiKeyDetails.from(api_key_record);
    }

    public ApiKeyDetails get(UUID app_id, UUID api_key_id) {
        requireApplication(app_id);
        return ApiKeyDetails.from(keys.findByIdAndApplicationId(api_key_id, app_id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found")));
    }

    @Transactional
    public IssuedApiKey rotate(UUID app_id, UUID api_key_id) {
        access.lockedApplication(app_id);
        var old_key = lockedKey(app_id, api_key_id);
        if (old_key.getRevokedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Revoked API keys cannot be rotated");
        }
        var replacement = issue(app_id, old_key.getName());
        old_key.replaceWith(replacement.key().id());
        return replacement;
    }

    @Transactional
    public void revoke(UUID app_id, UUID api_key_id) {
        access.lockedApplication(app_id);
        lockedKey(app_id, api_key_id).revoke();
    }

    // Authentication boundary for ingestion and event-status reads.
    public UUID authenticate(String api_key) {
        if (api_key == null || !api_key.matches("er_[A-Za-z0-9_-]{43}")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key");
        }
        // Only unrevoked key hashes authenticate. Revocation takes effect on the
        // next API-key request; it neither invalidates dashboard JWTs nor cancels
        // queued deliveries.
        UUID app_id = keys.findByKeyHashAndRevokedAtIsNull(hash(api_key)).map(ApiKey::getApplicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key"));
        try {
            access.application(app_id);
        } catch (ResponseStatusException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key");
        }
        return app_id;
    }

    private IssuedApiKey issue(UUID app_id, String name) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String api_key = "er_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var api_key_record = new ApiKey(app_id, name, hash(api_key), api_key.substring(0, 11));
        return new IssuedApiKey(ApiKeyDetails.from(keys.saveAndFlush(api_key_record)), api_key);
    }

    private static String hash(String api_key) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(api_key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private ApiKey lockedKey(UUID app_id, UUID api_key_id) {
        return keys.findForUpdate(api_key_id, app_id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found"));
    }

    private void requireApplication(UUID app_id) {
        access.application(app_id);
    }
}
