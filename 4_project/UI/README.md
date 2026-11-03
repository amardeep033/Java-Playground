# Event Relay UI

React 19 + TypeScript + Vite. Requires Node.js 22.12+.

If your terminal uses Node 18 (`node --version`), run from `UI/`:

```bash
npx --yes --package=node@22 --call 'npm run dev'
```

This uses Node 22 for the command without changing your system Node version. The same wrapper works for other scripts, e.g. `npx --yes --package=node@22 --call 'npm run build'`.

```bash
# Terminal 1: from service-a/
mvn spring-boot:run

# Terminal 2: from UI/
npm ci
npm run dev
```

Open http://localhost:5173. Register an account or sign in with your registered email and password. Existing accounts continue to work.

- Applications, event types, endpoint subscriptions, API keys (create/rotate/revoke), profile and password changes.
- Soft-delete users/apps/event types/endpoints with confirmation; deleting a parent includes its children. API-key revocations retain history.
- Endpoint configuration: enabled status, rate limit, concurrency, timeout, maximum attempts. Service B applies these settings to newly queued deliveries.
- Signing secrets are generated on endpoint creation, can be rotated, and appear once. Metadata never reveals a secret.
- Events: send JSON payloads, browse/search/filter received events, and inspect event delivery history.
- Deliveries: live status, request inspection, attempt timelines, bounded response previews, and failed-delivery replay with linked history.
- Dead letters: persisted failed deliveries; optionally include already replayed failures.
- Dashboard activity uses the HttpOnly JWT session; refreshing stays signed in. API keys are for external event clients.
- USER can access owned resources; ADMIN can access all application workspaces. Session lifetime: 8 hours; sign out/current or all sessions in the UI.
- Vite proxies `/api` to `http://localhost:8081`; no backend CORS changes needed. Override with `API_PROXY_TARGET`.
- One-time key secrets stay in local component memory. JWT is in an HttpOnly cookie; no API key or JWT in local/session storage.
- Profile & security shows recent login attempts. Five failures within 15 minutes lock login for 15 minutes. Password change signs out all sessions.
- Stack and dependencies follow `../mindmap/frontend`; charts and shared client stores are omitted because current features do not require them.

```bash
npm run build
npm test
# With Service A running, and Chrome installed:
npm run test:e2e
```

E2E tests use UI port 5174 so a development server on 5173 can keep running. They register a disposable user. To identify the exact email, set `E2E_RUN_ID` before running; it becomes `ui-e2e-<E2E_RUN_ID>@example.com`. Test data persists unless removed manually.

Build output: `dist/`. For static hosting, serve `index.html` for client routes and reverse-proxy `/api/` to Service A. `VITE_API_BASE_URL` optionally overrides the API URL at build time; cross-origin deployment requires matching backend CORS configuration.

`PLAYWRIGHT_CHROME_PATH` overrides the browser executable (default `/usr/bin/google-chrome`).

From the project root: `./run0.sh` restarts this UI; `./run0.sh --stop` stops it. Use `UI_PORT` for a custom port.

Full-stack verification (disposable PostgreSQL database, RabbitMQ container and local receiver; existing services/data are untouched):

```bash
# Build both services from the project root first:
mvn -f service-a/pom.xml package
mvn -f service_b/pom.xml package
# With Node 22+, Docker, psql and local PostgreSQL available:
python3 UI/e2e/run-local.py
```

The full-stack runner covers sending, status/attempt projection, replay idempotency, history synchronization, response bounds, pagination, and the browser workflow. It removes its own test database and broker afterward.

Authentication configuration (Service A): AUTH_COOKIE_SECURE=true behind HTTPS. Keep AUTH_KEY_FILE persistent (local default: service-a/.local/auth.key), or set AUTH_JWT_KEY to a Base64-encoded 32-byte key. CSRF is handled automatically by the UI. Role bootstrap and curl examples: ../service-a/tables.md and ../service-a/api.md.
