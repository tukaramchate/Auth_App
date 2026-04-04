# Auth App Implementation Remediation Plan

## Current Status (April 2026)
- Overall progress: **Phase 0-5 largely completed**, **Phase 6-7 partially pending**.
- Backend compile: ✅ Passing
- Backend tests: ✅ Passing (current baseline test suite)
- Frontend lint: ✅ Passing
- Frontend build: ✅ Passing

## Objective
Deliver a secure, stable, and production-ready authentication platform by resolving all critical, high, and medium audit findings in a single coordinated implementation program.

## Scope
This plan covers:
- Backend security and authorization hardening
- Token and session model hardening
- API request/response contract cleanup
- Frontend auth resilience and quality improvements
- Environment and deployment configuration hardening
- Testing, CI gates, and operational governance

## Delivery Strategy
- Work in one hardening branch: `hardening/full-remediation`
- Use phased implementation with security-first ordering
- Merge only when phase exit criteria are met
- Enforce CI gates before every merge

## Phase 0: Baseline Stabilization
### Tasks
1. Create hardening branch and freeze feature merges during remediation.
2. Add mandatory CI checks for:
   - Backend compile
   - Backend tests
   - Frontend build
   - Frontend lint
3. Define measurable success criteria and risk dashboard.

### Exit Criteria
- CI pipeline exists and fails on any compile/test/lint/build issue.
- Team aligns on remediation sequence and ownership.

### Status
- ✅ Done (local verification gates are passing for compile/test/lint/build)

## Phase 1: Immediate Critical Security Fixes
### 1.1 Authorization Matrix Correction
#### Tasks
1. Restrict all `/api/v1/users/**` endpoints to admin by default.
2. Introduce explicit self-service endpoints (for example `/api/v1/users/me`).
3. Add ownership checks for all self-service read/update operations.
4. Add access-control integration tests for:
   - Admin allowed
   - Non-admin denied
   - User self-access allowed
   - User cross-account denied

#### Exit Criteria
- No user-management endpoint is reachable by unintended principals.

#### Status
- ✅ Done (admin restriction for `/api/v1/users/**` with explicit `/api/v1/users/me` self endpoint)

### 1.2 Password Update Hardening
#### Tasks
1. Remove direct password assignment in update flow.
2. Centralize password changes into one service method.
3. Enforce password policy validation before save.
4. Ensure bcrypt hashing is always applied server-side.
5. Add tests asserting stored password is hashed.

#### Exit Criteria
- Plaintext password storage path is eliminated.

#### Status
- ✅ Done (password update flow now encodes using BCrypt)

### 1.3 Sensitive Logging Elimination
#### Tasks
1. Remove logging of refresh tokens, authorization headers, and OAuth raw attributes.
2. Add safe logging utility for redaction (token prefix only, masked values).
3. Lower security logging verbosity in non-debug environments.
4. Add CI grep check for sensitive log patterns.

#### Exit Criteria
- No secret/PII material appears in normal runtime logs.

#### Status
- ✅ Done (sensitive token/header/profile logs removed or reduced)

## Phase 2: Token and Session Model Hardening
### 2.1 Refresh Token Exposure Reduction
#### Tasks
1. Remove refresh token from response body contract.
2. Keep refresh token only in `HttpOnly` secure cookie.
3. Update backend DTOs and controllers accordingly.
4. Update frontend typing and response consumers.

#### Exit Criteria
- Refresh token is never exposed to JS runtime via API body.

#### Status
- ✅ Done (refresh token removed from API response body; cookie-based refresh maintained)

### 2.2 Refresh Interceptor Deadlock Fix
#### Tasks
1. Exclude `/auth/refresh` from refresh-retry interceptor logic.
2. Use separate axios instance for refresh endpoint.
3. Keep request queue for non-refresh calls only.
4. Add concurrency tests for parallel 401 scenarios.

#### Exit Criteria
- No deadlock or infinite retry during token refresh failure.

#### Status
- ✅ Done (separate refresh client + interceptor refresh-endpoint bypass)

### 2.3 Frontend Token Persistence Hardening
#### Tasks
1. Stop persisting access token in local storage.
2. Keep auth state minimal and non-sensitive in persistence.
3. Rehydrate session via refresh endpoint on app start.

#### Exit Criteria
- Access token is memory-only and not persisted across browser storage.

#### Status
- ✅ Done (access token removed from persisted store; silent refresh hydration added)

## Phase 3: API Contract and Validation Hardening
### 3.1 DTO Segregation and Data Minimization
#### Tasks
1. Split request DTOs from response DTOs.
2. Remove password and internal-only fields from response models.
3. Replace broad mapping with explicit mapping for sensitive models.

#### Exit Criteria
- API responses do not expose password/hash or unnecessary internals.

#### Status
- 🟡 In Progress
- Completed: password made write-only in DTO serialization; refresh token removed from response body.
- Remaining: full DTO segregation (request vs response classes for all user/auth endpoints).

### 3.2 Request Validation Enforcement
#### Tasks
1. Add Bean Validation constraints to auth/user request DTOs.
2. Use `@Valid` in all controller request bodies.
3. Standardize validation error response shape.

#### Exit Criteria
- Invalid inputs are consistently rejected with clear 400 responses.

#### Status
- ✅ Done (validation annotations + `@Valid` applied in core auth flows)

### 3.3 Exception Handler Correctness
#### Tasks
1. Fix handler method signature mismatch for `IllegalArgumentException`.
2. Add tests for each mapped exception path.

#### Exit Criteria
- Exception mapping behavior is deterministic and tested.

#### Status
- ✅ Done (IllegalArgumentException handler signature corrected)

## Phase 4: Environment and Configuration Hardening
### 4.1 Profile-Specific Config Completion
#### Tasks
1. Populate QA and PROD application configuration files.
2. Move all secrets to environment/secret manager.
3. Set explicit prod-safe values for logging, CORS, and rate limits.

#### Exit Criteria
- QA/PROD boot with explicit secure configuration.

#### Status
- ✅ Done (QA/PROD config baselines populated)

### 4.2 Unsafe Default Removal
#### Tasks
1. Disable bootstrap admin in non-dev profiles.
2. Remove fallback admin passwords from config defaults.
3. Replace `ddl-auto: update` with migration-driven schema changes.
4. Add Flyway or Liquibase migration baseline.

#### Exit Criteria
- No insecure fallback credentials or auto-mutation schema settings in deployable profiles.

#### Status
- 🟡 In Progress
- Completed: bootstrap admin defaults hardened, fallback admin passwords removed, secure env variables aligned.
- Remaining: migration framework adoption (Flyway/Liquibase) and schema migration baseline.

## Phase 5: Frontend Quality and UX Corrections
### Tasks
1. Resolve lint backlog (`any`, unused vars, empty blocks, hook deps warnings).
2. Fix OAuth failure route mapping to dedicated failure page.
3. Remove debug console logging from production code paths.
4. Add user-visible retry guidance for login/refresh errors.

### Exit Criteria
- Frontend lint passes with zero errors.
- OAuth success/failure routes behave correctly.

### Status
- ✅ Done (lint clean, OAuth failure route corrected, debug logs cleaned)

## Phase 6: Test and CI Hardening
### 6.1 Backend Test Health Restoration
#### Tasks
1. Fix security filter ordering issue causing context-load failures.
2. Add integration tests for:
   - Auth login/refresh/logout
   - Role-based access
   - Rate limiting 429 behavior
3. Add regression tests for password update and DTO exposure constraints.

#### Exit Criteria
- Backend test suite is stable and green.

#### Status
- 🟡 In Progress
- Completed: context-load and existing baseline tests pass.
- Remaining: dedicated integration tests for auth matrix and rate-limit scenarios.

### 6.2 Frontend Reliability Tests
#### Tasks
1. Add tests for interceptor retry/queue behavior.
2. Add tests for OAuth callback and failure routes.
3. Add tests for guarded admin/user routes.

#### Exit Criteria
- Critical auth flows are covered by automated tests.

#### Status
- ⏳ Pending

### 6.3 CI Policy Enforcement
#### Tasks
1. Block merge on failed tests/lint/build.
2. Add security checks:
   - secret scanning
   - log-leak pattern checks
   - dependency vulnerability scan

#### Exit Criteria
- CI enforces both quality and security policies.

#### Status
- ⏳ Pending

## Phase 7: Release Readiness and Governance
### Tasks
1. Create release checklist with sign-off gates.
2. Prepare runbook for auth incidents and token compromise response.
3. Create architecture decision record for:
   - token strategy
   - endpoint authorization policy
   - profile/config governance
4. Conduct post-remediation audit review.

### Exit Criteria
- Team has operational runbooks and governance artifacts in place.

### Status
- ⏳ Pending

## Work Breakdown and Ownership
- Security lead: authorization model, token strategy, logging redaction
- Backend lead: DTO refactor, validation, exception handling, tests
- Frontend lead: interceptor refactor, persistence hardening, lint cleanup
- DevOps lead: CI gates, secrets policy, profile configs, release governance

## Suggested Timeline
- Week 1: Phases 0-2
- Week 2: Phases 3-4
- Week 3: Phases 5-6
- Week 4: Phase 7 + stabilization buffer

## Risks and Mitigations
1. Risk: Breaking auth flows during DTO/token changes.
   - Mitigation: contract tests + phased rollout + backward compatibility window.
2. Risk: Access control changes block legitimate paths.
   - Mitigation: explicit endpoint matrix and role-based integration tests.
3. Risk: CI noise from legacy lint debt.
   - Mitigation: fix by module, enforce no-new-debt rule from day one.

## Definition of Done
1. No critical/high audit findings remain open.
2. Backend compile/test and frontend build/lint pass in CI.
3. No sensitive token/PII logging remains.
4. Access control matrix is explicit and tested.
5. QA and PROD configurations are complete and secure.
6. Core auth flows are covered by regression tests.

## What Is Already Completed In Code
1. Authorization hardening with explicit self endpoint.
2. Password update encryption fix.
3. Token exposure reduction (refresh token removed from body).
4. Validation enforcement for key auth requests.
5. Sensitive logging cleanup.
6. Frontend refresh flow hardening and deadlock prevention.
7. Frontend token persistence hardening.
8. Frontend lint/build health recovery.
9. Env configuration alignment and secure defaults.

## Immediate Next Actions
1. Add backend integration tests for:
   - user authorization matrix (`/users/**`)
   - rate-limit 429 behavior on auth endpoints
2. Add frontend auth-flow tests for interceptor refresh queue behavior.
3. Adopt Flyway or Liquibase and create initial migration baseline.
4. Add CI security checks (secret scan + dependency vulnerability scan + log-leak patterns).
