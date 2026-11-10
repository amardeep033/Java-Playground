# Event Relay UI

React 19, TypeScript and Vite. Requires Node 22.12+.

From the project root:

```bash
./run.sh
./run1.sh
./run2.sh
./run0.sh
```

Open http://localhost:5173. Company signup requires your name, company name, email and password, and creates the first ADMIN. Existing company members use Sign in on the same page; there is no public normal-user registration or developer/admin-console login.

- ADMIN: all company applications; create accounts as ADMIN/USER; edit account names/emails/roles; reset passwords; map users to apps. Keep at least one ADMIN.
- USER: mapped applications only. No company user administration, app creation, or app metadata editing.
- Profile: own name/password, deletion with last-admin protection, recent login attempts.
- Monitoring: Incoming requests → Events → Deliveries → Dead letters. Configuration: API keys, Event types, Endpoints. Both groups collapse.
- ADMIN-only Workspace: Users & access (including app mappings), Applications, Audit logs. One audit screen filters current app + company, current app only, company only, or all company apps; search/resource/action filters run on the backend. No credential values are recorded.
- Profile & security opens as two overlay cards from the bottom profile button. User editors expose fields, mappings and password reset in one layout. List names/IDs open details; no arrow-only actions. Resource management stays behind pencils; deletion requires red confirmation. App context persists; pagination stays at the bottom.
- Access JWT: 15 minutes by default. Opaque refresh cookie: fixed maximum 30 days, rotates on use; reuse/logout revokes its family. Password changes revoke all account refresh families. Access JWT verification remains stateless; business authorization checks live company permissions.
- JWT/refresh/CSRF cookies are HttpOnly; writes require a masked CSRF header. Explicit API-key event/monitoring requests use separate key checks. Credentials never enter browser storage. Only the selected app ID and theme preference are stored.
- Vite proxies `/api` to Service A on 8081. Service B has no public HTTP APIs.

```bash
cd UI
npm run build
npm test
```

Full-stack checks use disposable PostgreSQL and RabbitMQ; build both service jars first:

```bash
python3 UI/e2e/run-local.py
```

API curls and table examples: [Service A APIs](../service-a/api.md), [Service A tables](../service-a/tables.md).

Delivery monitoring reads Service A projections: pending includes Service B’s internal retries. Final SUCCEEDED/DEAD completions include all attempts; A repairs missed completions every 60 seconds through an authenticated REST cursor feed. No direct UI access to Service B.

Users & access: paginated users include applicationIds and the complete company app catalogue. Create/edit use a searchable multi-select dropdown; no separate user-app list request or app-mapping pagination.

Application activation uses its metadata update PUT. Event types/endpoints keep separate activation APIs because metadata and activation have different permissions. Activation preserves metadata; backend permissions and audit rules remain enforced.


Related records use a dropdown to open the destination list with a filter/selection. Detail cards load only their selected record. Audit logs use one company-admin API with server-side column filters and time sorting.

Endpoint destination, configuration and event mappings update in place with audit entries. Previously accepted deliveries retain their original dispatch snapshot, including retry settings and signing credentials.

List filters live in column headers and close on outside click/Escape. Status selections filter on the backend before pagination. Editors use horizontal labels, header activation switches, and right-aligned actions; delete effects are described in confirmation dialogs. Event subscriptions use a searchable, paginated multi-select.

Endpoint writes use two PUT routes: common metadata/delivery configuration, and ADMIN-only activation/destination/subscriptions. Event-type pickers load more options on scroll, with search and no pagination buttons. API-key status is in the editor header; revocation remains irreversible and requires confirmation.

For a focused UI rerun after backend contracts pass: `python3 UI/e2e/run-local.py --browser-only polish.spec.ts` (from the project root, Node 22+ on PATH). It still uses disposable services/data.
