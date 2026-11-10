# Event Relay

- Read `mindmap/rough_idea` for project context
- Work prompt by prompt. Do not add later phases (shared Redis limiting, telemetry, gRPC, session auth or hosting) unless requested.
- Keep `service-a/api.md` and `service-a/tables.md` concise and aligned with code; put complete curl examples in API table rows and sample data in tables.md. Maintain same for service b.
- For UI work, follow `mindmap/frontend` and keep current backend capabilities reflected in the UI.
- Keep exactly four self-contained launch scripts: `run.sh` (infrastructure), `run0.sh` (UI), `run1.sh` (A), `run2.sh` (B). Each owns stop/restart handling at the top; no separate kill scripts or shell helpers.
- `test.sh` is an explicitly requested exception: a standalone customer webhook receiver for retry/DLQ testing.

- Dashboard authentication uses stateless short-lived JWTs in HttpOnly SameSite=Lax cookies, with rotating hashed refresh-token families. Only users.role stores ADMIN/USER; users.client_id is required. There is no developer/platform role, global console, or cross-company access. Public signup requires person name + company name and creates the first ADMIN. Only company ADMIN creates users/admins, changes email/roles, resets other passwords, and manages app mappings. ADMIN accesses every company app; USER accesses mapped apps. Both use the same login. Enforce live company/role/mapping checks on the backend. Preserve at least one ADMIN per company.
- Schemas are fresh canonical CREATE definitions, not legacy upgrade chains. clients owns users/applications; workspace_members composite client FKs prevent cross-company mappings. All account/resource audit rows require client_id. App name/description/activation are ADMIN-only; other updates follow service-a/tables.md.
- Protect all cookie-authenticated writes (including registration/login/refresh/logout) with CSRF. Only explicit API-key event/monitoring routes bypass CSRF; an invalid key never falls back to the JWT cookie.
- Never expose JWTs to JavaScript or store credentials/tokens in browser storage. Login audit records login outcomes; audit_logs records requested resource changes. Never include passwords/hashes, API-key values, or signing-secret values in audit details/history responses; keep secrets out of logs.
- Access JWTs remain stateless and valid until expiry. Refresh tokens are random opaque HttpOnly cookies; store only SHA-256 hashes in DB, rotate on every use, revoke the family on reuse/logout, and revoke all user families on password change/account deletion. Never extend the family’s absolute lifetime beyond 30 days; cap renewed access JWTs at family expiry. Protect refresh with CSRF.

- Event-submission audit records sanitized request metadata/reasons, including pre-ingestion rejection. Never store raw request bodies or credential/header values. Rejected submissions are not deliveries/dead letters and cannot be replayed. Inactive endpoints are skipped destinations on accepted events.

- Endpoint URL/configuration/signing/subscription changes update in place with sanitized audit records; there are no endpoint configuration versions. Accepted deliveries and retries keep immutable dispatch snapshots captured at acceptance; later application/endpoint/subscription changes affect only new requests.

- Endpoint updates use exactly two PUT routes: common name/description/delivery configuration, and ADMIN-only activation/URL/event mappings. Activation is never accepted inside common delivery configuration. Form selectors use search and scrolling, without pagination controls.
