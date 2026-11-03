# Event Relay

- Read `mindmap/rough_idea` for project context
- Work prompt by prompt. Do not add later phases (shared Redis limiting, telemetry, gRPC, session auth or hosting) unless requested.
- Keep `service-a/api.md` and `service-a/tables.md` concise and aligned with code; put complete curl examples in API table rows and sample data in tables.md. Maintain same for service b.
- For UI work, follow `mindmap/frontend` and keep current backend capabilities reflected in the UI.
- Keep exactly four self-contained launch scripts: `run.sh` (infrastructure), `run0.sh` (UI), `run1.sh` (A), `run2.sh` (B). Each owns stop/restart handling at the top; no separate kill scripts or shell helpers.

- Dashboard authentication uses HttpOnly JWT cookies backed by revocable DB sessions. USER owns their resources; ADMIN can manage application workspaces. Enforce ownership on the server.
- Protect cookie-authenticated writes with CSRF. Only explicit API-key event/monitoring requests may bypass CSRF; never fall back to a cookie when an explicit key is invalid.
- Keep passwords, JWTs and API-key secrets out of logs and browser storage. Audit login outcomes only. Password changes revoke all dashboard sessions.
