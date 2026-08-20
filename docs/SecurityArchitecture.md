# AegisTerra Security & Identity Architecture

**Version:** 2.0.0 (Phase 2 design)  
**Status:** Approved for implementation  
**Audience:** Backend, frontend, security, and operations stakeholders  
**Supersedes:** Security Architecture checklist baseline (0.1.0)

This document is the **design gate** for Phase 2. Implementation must conform to it. Deviations require an ADR.

---

## 1. Security Goals

1. Protect farmer, policy, claims, and financial data under least privilege.
2. Provide **auditable** authentication and authorization for government oversight.
3. Support multi-actor national operations (insurers, government, aggregators, field officers, farmers).
4. Remain **extensible** for MFA and enterprise SSO without rewriting the core.
5. Eliminate mock authentication; no dual auth stories (JWT vs HTTP Basic for SPA).

---

## 2. Authentication Flow

### 2.1 Primary login (password)

```
Browser                    API                         Store
   |                        |                            |
   |-- POST /auth/login --->|                            |
   |   (JSON credentials)   |-- validate lockout ------->|
   |                        |-- verify password (BCrypt)-|
   |                        |-- create LoginSession ---->|
   |                        |-- issue access JWT --------|
   |                        |-- issue refresh token ---->|
   |                        |-- write AuditLog --------->|
   |<-- 200 + Set-Cookie ---|                            |
   |   (HttpOnly cookies)   |                            |
   |   + body: user/me      |                            |
```

**Rules:**

- Credentials are sent once over HTTPS in the JSON body.
- **Access token** and **refresh token** are delivered primarily as **HttpOnly, Secure, SameSite** cookies (see §5).
- Response body returns identity projection (`/auth/me` shape), not long-lived secrets in JS-readable storage.
- Failed attempts increment failure counters and may lock the account (§8).

### 2.2 Authenticated API call

```
Browser -- Cookie: access_token --> API
API: validate JWT signature, exp, issuer, status of user
API: load authorities (roles/permissions) from token claims and/or cache
API: authorize method/route
```

If access token is expired/missing but refresh cookie is valid:

```
Browser/Interceptor -- POST /auth/refresh (refresh cookie) --> API
API: rotate refresh, revoke old, issue new cookie pair
Retry original request
```

### 2.3 Logout

```
POST /auth/logout
- Revoke current refresh token family (or current token)
- Invalidate LoginSession
- Clear auth cookies (Max-Age=0)
- Audit LOGOUT
```

### 2.4 Password change / reset

| Flow | Endpoint | Notes |
|------|----------|-------|
| Change (authenticated) | `POST /auth/change-password` | Requires current password; invalidates other sessions optionally |
| Forgot | `POST /auth/forgot-password` | Always returns generic success; emails/SMS reset token (adapter) |
| Reset | `POST /auth/reset-password` | Consumes one-time token; rotates password; revokes sessions |

Reset tokens are **opaque**, stored hashed, short-lived, single-use.

---

## 3. Authorization Model

### 3.1 Model: RBAC + permissions

```
User 1─* UserRole *─1 Role 1─* RolePermission *─1 Permission
```

- **Role:** named job function (e.g. `INSURANCE_OFFICER`).
- **Permission:** fine-grained capability string (e.g. `farmers:read`, `claims:approve`).
- Users receive permissions **only through roles** in Phase 2 (no direct user-permission grants yet; extensible later).

### 3.2 Standard roles (seeded)

| Code | Display name | Typical scope |
|------|--------------|---------------|
| `SYSTEM_ADMIN` | System Administrator | Full platform + IAM |
| `INSURANCE_ADMIN` | Insurance Administrator | Insurer org administration |
| `INSURANCE_OFFICER` | Insurance Officer | Policies, claims ops |
| `FI_OFFICER` | Financial Institution Officer | Premiums/payouts views |
| `GOVERNMENT_ANALYST` | Government Analyst | Read-heavy reporting |
| `AGGREGATOR` | Aggregator | Farmer onboarding cohorts |
| `FARMER` | Farmer | Self-service (future portal) |
| `AUDITOR` | Auditor | Read + audit logs |
| `SUPPORT` | Support | Limited user assistance |

### 3.3 Permission naming

Pattern: `{resource}:{action}`  
Examples: `users:read`, `users:write`, `roles:read`, `auth:admin`, `farmers:read`, `claims:write`.

Phase 2 seeds IAM permissions fully; domain permissions for farmers/farms/policies/claims are **seeded as placeholders** for later modules but enforced when those APIs appear.

### 3.4 Enforcement points

| Layer | Mechanism |
|-------|-----------|
| HTTP security filter | Authenticated vs anonymous; public auth routes |
| Method security | `@PreAuthorize("hasAuthority('users:write')")` |
| Frontend routes | Permission-aware guards + nav filtering |
| Data scope (future) | Organization/tenant filters — ADR later; not Phase 2 multi-tenant isolation |

### 3.5 Permission caching

- JWT access token carries **role codes** and optionally a compact permission set (or role codes only).
- Prefer **role codes in JWT** + server-side permission resolution cache (Caffeine/in-memory) keyed by `userId` + `tokenVersion`/`permVersion`.
- On role/permission change: bump user `token_version` (or `security_stamp`) so existing access tokens fail validation until refresh/re-login.

---

## 4. Token Lifecycle

### 4.1 Access token (JWT)

| Property | Value |
|----------|-------|
| Type | Signed JWT (HS256 initially; RS256 ready via config) |
| Lifetime | Short (default **15 minutes**, configurable) |
| Claims | `sub` (userId), `email`/`username`, `roles[]`, `tv` (token version), `sid` (session id), `iss`, `iat`, `exp`, `jti` |
| Storage (browser) | **HttpOnly cookie** `at` (not `localStorage`) |
| Validation | Signature, exp, issuer, user active, `tv` matches user |

Access tokens are **not** persisted server-side (stateless), except optional denylist for emergency revoke (session/user version covers most cases).

### 4.2 Refresh token

| Property | Value |
|----------|-------|
| Type | Opaque random (256-bit), stored **hashed** (SHA-256) in DB |
| Lifetime | Default **7 days** (configurable); optional “remember me” **30 days** |
| Storage (browser) | **HttpOnly cookie** `rt`, path scoped to `/api/v1/auth` |
| Rotation | **Mandatory** on every refresh: old token revoked, new issued |
| Reuse detection | If revoked/rotated token is presented again → revoke **entire token family** + lock session |

### 4.3 Token family

Each login creates a `family_id`. All rotated refresh tokens share it. Reuse or logout revokes the family.

---

## 5. Refresh Token Strategy & Cookie Design

### 5.1 Cookies

| Cookie | HttpOnly | Secure | SameSite | Path | Notes |
|--------|----------|--------|----------|------|-------|
| `at` (access) | Yes | Yes (prod) | `Lax` or `Strict` | `/` | Short-lived |
| `rt` (refresh) | Yes | Yes (prod) | `Strict` preferred | `/api/v1/auth` | Refresh/logout only |

Local/dev may set `Secure=false` behind profile flag when not on HTTPS.

### 5.2 CSRF readiness

Because cookies are used:

- Prefer **SameSite=Strict/Lax** to reduce CSRF surface.
- For state-changing auth endpoints that rely on cookies, require custom header `X-Requested-With: XMLHttpRequest` or double-submit CSRF token when SameSite is insufficient (cross-site deployments).
- Spring CSRF: enable for cookie-authenticated browser clients in prod profile; SPA sends CSRF header from cookie-readable CSRF token **or** use SameSite=Strict + same-origin proxy.

**Phase 2 default:** same-origin Vite proxy + `SameSite=Lax` access + `Strict` refresh; CSRF token endpoint reserved for hardening (Phase 10 may tighten).

### 5.3 CORS

- Explicit allowed origins from config (`aegisterra.security.cors.allowed-origins`).
- `allowCredentials=true` when cookie auth is used.
- No `*` origin with credentials.

### 5.4 Why not localStorage

XSS can exfiltrate JS-readable tokens. HttpOnly cookies are not script-accessible. Access token in memory-only was considered; cookie pair chosen for simpler SPA refresh and alignment with government browser deployments. ADR-002 remains in force; storage detail is fixed here.

---

## 6. Password Policy

| Rule | Default |
|------|---------|
| Minimum length | 12 |
| Uppercase | ≥1 |
| Lowercase | ≥1 |
| Digit | ≥1 |
| Special character | ≥1 |
| Disallow username/email substring | Yes |
| Password history | Last **5** hashes (`password_history`) |
| Hash algorithm | BCrypt (strength **12**) |
| Temporary passwords | Force change on next login (`must_change_password`) |

Policy enforced in application layer on register/change/reset.

---

## 7. Session Management

### 7.1 LoginSession entity

Represents an authenticated browser/device session:

- Links user, refresh family, IP, user-agent, created/last-seen, expiry, status (`ACTIVE`/`REVOKED`/`EXPIRED`)
- Logout and reuse detection mark session `REVOKED`
- Idle timeout: optional server check on refresh (`last_seen_at` + idle window)
- Absolute timeout: session `expires_at`

### 7.2 Concurrent sessions

- Default: multiple sessions allowed.
- Configurable max sessions per user; oldest revoked when exceeded (admin setting later).

### 7.3 Remember Me

- Login request flag `rememberMe=true` extends refresh TTL (e.g. 30d vs 7d).
- Does **not** lengthen access token TTL.
- Audited.

---

## 8. Account Lifecycle

| Status | Meaning |
|--------|---------|
| `PENDING` | Created, not yet activated |
| `ACTIVE` | Can authenticate |
| `LOCKED` | Temporary lock after failures or admin |
| `SUSPENDED` | Admin disabled |
| `DISABLED` | Soft end-of-life |
| `DELETED` | Soft-deleted (`deleted=true`); no login |

**Lockout:** after **N** consecutive failures (default 5) within window → `LOCKED` until `locked_until` or admin unlock.  
**Unlock:** automatic after lock duration (default 30 minutes) or `SYSTEM_ADMIN` action.

Activation tokens (email) are Phase 2–ready in schema; email delivery may be stubbed with audit + logged token in `local` only.

---

## 9. Audit Strategy

### 9.1 Security audit events (mandatory)

Persist to `audit_logs` (append-only from app perspective):

- `LOGIN_SUCCESS`, `LOGIN_FAILURE`
- `LOGOUT`
- `TOKEN_REFRESH`, `TOKEN_REUSE_DETECTED`
- `PASSWORD_CHANGE`, `PASSWORD_RESET_REQUEST`, `PASSWORD_RESET_SUCCESS`
- `ACCOUNT_LOCKED`, `ACCOUNT_UNLOCKED`
- `USER_CREATED`, `USER_UPDATED`, `USER_STATUS_CHANGED`
- `ROLE_ASSIGNED`, `ROLE_REVOKED`

Each event: actor user id (nullable), target id, action, resource type, IP, user-agent, correlation id, payload JSON (no raw passwords/tokens), timestamp.

### 9.2 Correlation IDs

Existing `X-Correlation-Id` filter remains; all auth audits store correlation id for SIEM join.

### 9.3 Retention

Configurable retention policy (ops); Phase 2 stores indefinitely pending ops ADR.

---

## 10. MFA Readiness

Phase 2 does **not** require MFA enrollment UI, but the model must not block it:

- User flags: `mfa_enabled`, `mfa_enforced`
- Table stub or columns for `mfa_methods` (TOTP/WebAuthn) in a follow-up changelog
- Login flow extension point: after password OK, if MFA required → return `mfa_required` challenge id (no full session cookies until MFA OK)

**Implementation Phase 2:** columns + API contract hooks; challenge endpoints can return `501` or be omitted until Phase 10/11 — document clearly in OpenAPI as “planned”.

---

## 11. Future SSO Integration

- Local password IdP remains default.
- Future: OAuth2/OIDC (external government IdP) via Spring Authorization/Resource patterns.
- `users` table supports `external_subject` / `identity_provider` nullable columns for linking.
- Roles/permissions stay in AegisTerra; IdP supplies identity only unless mapped by claim bridge.

New ADR required before enabling SSO in production.

---

## 12. API Security Model

### 12.1 Public endpoints

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`
- `GET /api/v1/health`
- `GET /actuator/health/**`
- OpenAPI/Swagger (**profile-gated**; disabled or locked in `prod` by default)

### 12.2 Authenticated endpoints

All other `/api/v1/**` require valid access token (cookie or `Authorization: Bearer` for non-browser clients/tests).

### 12.3 Permission-protected examples

| Endpoint | Permission |
|----------|------------|
| `GET/POST /api/v1/users` | `users:read` / `users:write` |
| `GET /api/v1/roles` | `roles:read` |
| `GET /api/v1/permissions` | `permissions:read` |
| `POST /api/v1/auth/change-password` | Authenticated user (self) |

### 12.4 Transport & headers

- HTTPS in non-local profiles
- Security headers: `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Content-Security-Policy` (API-appropriate), `Cache-Control` for auth responses
- Input validation via Bean Validation on all write DTOs
- Global exception handler returns stable `ApiError` without stack traces in prod

### 12.5 Throttling

- Login / forgot-password / refresh: rate limited per IP + per username (Bucket4j or similar in-memory for Phase 2; Redis later)
- Exceeding limit → `429` + audit

### 12.6 HTTP Basic

**Removed** for application APIs. Tests use cookie/JWT or `@WithMockUser` / security test helpers. Actuator remains authenticated via JWT or separate management credentials (config).

---

## 13. Domain Entities (Identity Platform)

Detailed field-level design for implementers. All tables use UUID PK and standard audit columns unless noted.

### 13.1 User

**Purpose:** Human (or service-linked) account for authentication.  
**Relationships:** UserRole*, RefreshToken*, LoginSession*, PasswordHistory*, UserPreference?  
**Lifecycle:** `PENDING` → `ACTIVE` → (`LOCKED`|`SUSPENDED`) → `DISABLED`/`DELETED`  
**Validation:** unique email; unique username; password hash required for local accounts; status enum  
**Indexes:** unique(email), unique(username), index(status), index(locked_until)  
**Extensibility:** `external_subject`, `identity_provider`, `mfa_enabled`, `token_version`, org_id (nullable future)

### 13.2 Role

**Purpose:** Named set of permissions.  
**Relationships:** UserRole*, RolePermission*  
**Lifecycle:** `ACTIVE`/`DISABLED`  
**Validation:** unique code  
**Indexes:** unique(code)  
**Extensibility:** `is_system` flag prevents deletion of seeded roles

### 13.3 Permission

**Purpose:** Atomic authority string.  
**Relationships:** RolePermission*  
**Lifecycle:** `ACTIVE`/`DISABLED`  
**Validation:** unique code matching `{resource}:{action}`  
**Indexes:** unique(code), index(resource)

### 13.4 UserRole

**Purpose:** Assignment of role to user (optional validity window).  
**Relationships:** User, Role  
**Lifecycle:** active until revoked/soft-deleted  
**Validation:** unique(user_id, role_id) where not deleted  
**Indexes:** (user_id), (role_id), unique composite

### 13.5 RolePermission

**Purpose:** Grants permission to role.  
**Relationships:** Role, Permission  
**Validation:** unique(role_id, permission_id)  
**Indexes:** composite unique, (permission_id)

### 13.6 RefreshToken

**Purpose:** Persist hashed refresh tokens and rotation family.  
**Relationships:** User, LoginSession  
**Lifecycle:** `ACTIVE` → `ROTATED`/`REVOKED`/`EXPIRED`  
**Validation:** hash unique; expires_at required  
**Indexes:** unique(token_hash), index(family_id), index(user_id), index(expires_at)

### 13.7 LoginSession

**Purpose:** Track session metadata for revoke/audit/timeout.  
**Relationships:** User, RefreshToken*  
**Lifecycle:** `ACTIVE` → `REVOKED`/`EXPIRED`  
**Indexes:** (user_id, status), (expires_at)

### 13.8 PasswordHistory

**Purpose:** Prevent password reuse.  
**Relationships:** User  
**Lifecycle:** append-only; prune beyond policy N  
**Indexes:** (user_id, created_at)

### 13.9 AuditLog

**Purpose:** Security and admin audit trail.  
**Relationships:** optional actor User  
**Lifecycle:** append-only (no update/delete from app)  
**Indexes:** (created_at), (action), (actor_user_id), (correlation_id)

### 13.10 UserPreference (optional Phase 2)

**Purpose:** Locale, timezone, UI density.  
**Relationships:** 1–1 User  
**Indexes:** unique(user_id)

---

## 14. Database & Migrations

- **Engine:** PostgreSQL (local Docker or managed); tests use Testcontainers PostgreSQL **or** H2 in PostgreSQL mode only if spatial not required for IAM (prefer Testcontainers).
- **Migrations:** Liquibase changelogs under `db/changelog/`
- **IDs:** UUID
- **Audit columns on mutable tables:** `created_at`, `updated_at`, `created_by`, `updated_by`, `version`, `status`
- Soft delete: `deleted` boolean + filter in repositories
- Seed changelog: roles, permissions, role_permission map, bootstrap `SYSTEM_ADMIN` user via env password on first local boot (**not** hardcoded in VCS)

Phase 2 introduces persistence **for IAM**. PostGIS/farm spatial remains Phase 3 but shares the same PostgreSQL instance and Liquibase pipeline.

---

## 15. Backend Module Layout (Clean Architecture)

```
domain/identity/          # entities, enums, domain services (password policy)
application/identity/     # use cases: Login, Refresh, Logout, ManageUsers...
application/identity/port # repositories, TokenProvider, MailPort
infrastructure/persistence/ # JPA entities/adapters, Liquibase
infrastructure/security/    # JWT, filters, cookie writer, rate limiter
presentation/auth|user|role # controllers + DTOs
```

---

## 16. Frontend Security UX

- Professional login (no default passwords in production builds)
- Forgot / reset password pages
- Auth bootstrap: `GET /auth/me` with credentials (`credentials: 'include'`)
- Protected routes; redirect to login preserving return URL
- Permission-aware navigation
- Silent refresh on `401` via `/auth/refresh` once; then logout
- Session timeout warning (optional UX) based on access TTL
- Accessible forms (labels, errors, focus)
- Responsive layout
- **No** access/refresh tokens in `localStorage` / `sessionStorage`

---

## 17. Testing Requirements

| Type | Coverage |
|------|----------|
| Unit | Password policy, token hash/rotation rules, permission evaluation |
| Integration | Login → me → refresh → logout; lockout; RBAC denied/allowed |
| Security | Reuse detection; expired token; CSRF/CORS smoke; validation errors |
| Frontend | Guard redirect; login error states (Vitest/RTL as introduced) |

---

## 18. Open Decisions Locked by This Document

| Topic | Decision |
|-------|----------|
| Browser token storage | HttpOnly cookies (not localStorage) |
| Refresh | Opaque + rotation + family revoke on reuse |
| AuthZ | RBAC → permissions; method security |
| Password hash | BCrypt strength 12 |
| HTTP Basic for SPA | Removed |
| MFA/SSO | Ready columns/hooks; full UX later + ADR |

---

## 19. Out of Scope for Phase 2

- Farmer/Farm/Policy business modules
- PostGIS geometries
- Full email/SMS provider production wiring (port + local stub OK)
- Complete MFA enrollment UX
- Live external IdP SSO
- Fine-grained multi-tenant row isolation

---

## 20. Quality Gate (Phase 2 exit)

Phase 2 is complete only when:

1. Authentication works end-to-end with cookies.  
2. Refresh rotation works; reuse revokes family.  
3. RBAC and permissions enforced on APIs.  
4. Swagger documents auth/IAM APIs.  
5. Liquibase migrations apply cleanly.  
6. Automated tests pass.  
7. No mock JWT / in-memory-only SPA auth remains.  
8. This architecture and CHANGELOG reflect reality.

---

## 21. References

- ADR-002 JWT with refresh tokens  
- ADR-003 Clean Architecture  
- ADR-006 Modular monolith  
- `DevelopmentRoadmap.md` Phase 2  
- `TechnicalDebt.md` TD-001..003, TD-017  

**Next step after this document:** implement domain + Liquibase + security stack per §§13–16.
