# API Quality Features Implementation Plan

## Objective
Standardize API behavior across backend endpoints by implementing:
1. Pagination and filtering standards.
2. Unified error schema for all API failures.
3. Predictable API versioning and compatibility rules.

This plan is intentionally non-breaking by default and designed for phased rollout.

## Scope
### In scope
1. Introduce a shared pagination/filter contract for list endpoints.
2. Introduce one canonical error response schema across auth, users, admin, and future security endpoints.
3. Add backend helpers and frontend consumers for predictable API handling.
4. Add tests and migration guidance.
5. Define and enforce API versioning policy.

### Out of scope
1. Multi-major coexistence beyond one active version (`v1`) and one staging version (`v2`).
2. GraphQL migration.
3. Bulk endpoint redesign unrelated to pagination or error handling.

## Current Baseline
1. Some endpoints return plain objects, others return custom wrapper types.
2. Exception handlers return mixed payload formats (`ApiError`, `ErrorResponse`, and security entrypoint payloads).
3. List endpoints (for example users, sessions) are not consistently paginated or filterable.
4. API version prefix is path-based and currently only `v1` is used (`/api/v1/auth`, `/api/v1/users`).
5. No formal backward-compatibility or deprecation policy is documented.

## Current Audit Findings (April 2026)
1. Versioning is present but minimal:
  - Strength: path prefix (`/api/v1/*`) is consistently used for current controllers.
  - Gap: no documented process for introducing `v2` without breaking clients.
2. Error schema is not unified yet:
  - Security entrypoint returns `ApiError`.
  - Global exception handling mixes `ApiError` and `ErrorResponse`.
3. List APIs are not standardized:
  - `GET /api/v1/users` returns non-paginated iterable data.
  - Session list APIs also return raw lists with no pagination metadata.

## Target API Standards
## 1. Pagination and Filtering
### 1.1 Query Parameters (Standard)
For list endpoints:
1. `page` (default `0`)
2. `size` (default `20`, max `100`)
3. `sort` (example: `createdAt,desc`)
4. Endpoint-specific filters (example: `status`, `role`, `email`, `q`)

### 1.2 Response Envelope for Lists
```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 125,
  "totalPages": 7,
  "hasNext": true,
  "hasPrevious": false,
  "sort": "createdAt,desc",
  "filters": {
    "status": "ACTIVE",
    "role": "USER"
  }
}
```

### 1.3 Backend Implementation Notes
1. Use Spring Data `Pageable` in controllers/services.
2. Create shared DTO `PageResponse<T>`.
3. Keep default sort deterministic for each endpoint.
4. Validate and clamp `size` to prevent abuse.

## 2. Unified Error Schema
### 2.1 What this means in simple terms
Every API error should look the same, no matter where it comes from.

Benefits:
1. Frontend handles all errors in one place.
2. Users see clearer messages.
3. Debugging is faster because fields are predictable.

### 2.2 Canonical Error Response
```json
{
  "status": 400,
  "code": "VALIDATION_FAILED",
  "title": "Validation Error",
  "message": "One or more fields are invalid.",
  "path": "/api/v1/auth/register",
  "timestamp": "2026-04-04T18:22:30Z",
  "traceId": "<request-trace-id>",
  "details": [
    {
      "field": "email",
      "reason": "must be a well-formed email address"
    }
  ]
}
```

Field meaning:
1. `status`: HTTP status code.
2. `code`: Stable app-level error code used by frontend logic.
3. `title`: Short human-friendly category.
4. `message`: Main error message to show to the user.
5. `path`: Endpoint path that failed.
6. `timestamp`: UTC timestamp of error creation.
7. `traceId`: Request correlation ID for logs/troubleshooting.
8. `details`: Optional list of field-level errors.

### 2.3 Error Code Catalog (Initial)
1. `VALIDATION_FAILED`
2. `UNAUTHORIZED`
3. `FORBIDDEN`
4. `RESOURCE_NOT_FOUND`
5. `CONFLICT`
6. `RATE_LIMITED`
7. `BAD_REQUEST`
8. `INTERNAL_ERROR`

### 2.4 Exception-to-code mapping (must be consistent)
1. Validation exceptions -> `VALIDATION_FAILED` (400)
2. Auth failures (bad token/credentials) -> `UNAUTHORIZED` (401)
3. Permission failures -> `FORBIDDEN` (403)
4. Missing resources -> `RESOURCE_NOT_FOUND` (404)
5. Duplicate/business conflicts -> `CONFLICT` (409)
6. Rate limit blocks -> `RATE_LIMITED` (429)
7. Other bad request scenarios -> `BAD_REQUEST` (400)
8. Unhandled server errors -> `INTERNAL_ERROR` (500)

### 2.5 Backend implementation checklist
1. Replace mixed DTO usage with one `ApiErrorResponse` type.
2. Update `GlobalExceptionHandler` to return this shape for all mapped exceptions.
3. Update Spring Security `401` entrypoint and `403` access denied handler to the same shape.
4. Add validation field errors into `details`.
5. Never return stack traces or sensitive internals in responses.
6. Log full technical details server-side with `traceId`.

### 2.6 Make errors understandable for users
Rules for user-facing `message`:
1. Use simple words, short sentence, no backend jargon.
2. Tell the user what to do next when possible.
3. Do not expose internal details (SQL, stack trace, token parsing text).
4. Keep tone neutral and helpful.

Message examples:
1. `UNAUTHORIZED`:
  - "Your session expired. Please log in again."
2. `FORBIDDEN`:
  - "You do not have permission to perform this action."
3. `VALIDATION_FAILED`:
  - "Please check the highlighted fields and try again."
4. `RESOURCE_NOT_FOUND`:
  - "The requested item was not found."
5. `RATE_LIMITED`:
  - "Too many attempts. Please wait a minute and try again."
6. `INTERNAL_ERROR`:
  - "Something went wrong on our side. Please try again shortly."

Frontend handling note:
1. Show `message` directly for toast/alerts.
2. If `details` exists, show field errors inline on form fields.
3. Show optional `traceId` in advanced error UI only (not by default).

## 3. API Versioning Standard
### 3.1 Versioning Approach
1. Use path-based semantic major versioning: `/api/v{major}`.
2. Keep one stable major active (`v1`) while preparing next (`v2`) for breaking changes.
3. Non-breaking additions remain within current major.

### 3.2 Breaking vs Non-Breaking Rules
Non-breaking (same major):
1. Add optional request fields.
2. Add response fields that clients can ignore.
3. Add new endpoints.

Breaking (requires new major):
1. Remove or rename fields.
2. Change field types or meanings.
3. Change auth semantics or status-code behavior in incompatible ways.

### 3.3 Deprecation Policy
1. Mark old endpoints/fields as deprecated in docs and OpenAPI.
2. Emit deprecation warning header where feasible.
3. Keep deprecated behavior for at least one release window.
4. Remove only after migration guide is published and clients are updated.

## Backend Work Plan
## Phase A: Foundation
1. Create shared DTOs:
   - `PageResponse<T>`
   - `ApiErrorResponse`
   - `ApiErrorDetail`
2. Create mapper utility for `Page<T> -> PageResponse<T>`.
3. Add centralized `ErrorCode` enum.

## Phase B: Error Unification
1. Refactor `GlobalExceptionHandler` to standard response shape.
2. Add handlers for:
   - `MethodArgumentNotValidException`
   - `ConstraintViolationException`
   - `ResourceNotFoundException`
   - `IllegalArgumentException`
   - `BadCredentialsException` and auth-related exceptions
   - fallback `Exception`
3. Align Spring Security authentication entrypoint and access denied responses.

## Phase C: Pagination/Filtering Rollout
Apply standard pagination to endpoints in priority order:
1. Users list endpoint (`/api/v1/users`).
2. Admin session/security list endpoints.
3. Future audit/history lists.

For each endpoint:
1. Add `page`, `size`, `sort` params.
2. Add supported filters and validation.
3. Return `PageResponse<T>`.

## Phase D: Versioning Governance
1. Document versioning rules in API docs and repository docs.
2. Add PR checklist item: "Is this change breaking? If yes, require major version plan."
3. Add lightweight contract tests to validate `v1` response compatibility for critical endpoints.
4. Prepare `v2` namespace only when a validated breaking change is required.

## Frontend Work Plan
## Phase E: Client Adaptation
1. Add shared TS types:
   - `PageResponse<T>`
   - `ApiErrorResponse`
2. Normalize axios error handling to consume unified schema.
3. Update table/list screens for pagination controls and filter inputs.
4. Preserve backward compatibility during rollout with tolerant parsing.

## Migration Strategy (Non-Breaking)
1. Introduce v1-compatible wrappers first.
2. Roll endpoint-by-endpoint; avoid big-bang changes.
3. Keep current response fields temporarily where required, then deprecate.
4. Publish API contract notes in docs before enabling strict mode.
5. For any breaking change candidate, add a `v2` endpoint and keep `v1` behavior intact until migration completes.

## Testing Plan
## Backend
1. Unit tests for error mapper and page mapper.
2. Integration tests per endpoint:
   - pagination metadata correctness,
   - filter application correctness,
   - sort correctness,
   - error schema shape for 400/401/403/404/500.

## Frontend
1. API client tests for standardized error parsing.
2. UI tests for paginated list behavior.
3. Filter state and query serialization tests.

## Acceptance Criteria
1. All list endpoints in scope return `PageResponse<T>`.
2. All API failures return unified `ApiErrorResponse`.
3. Security and controller-thrown errors share identical schema.
4. Frontend consumes standardized errors without endpoint-specific branching.
5. Existing critical flows (login, register, user fetch) remain stable.
6. Versioning policy is documented and enforced in PR/release workflow.
7. No breaking API change is shipped in `v1` without an approved `v2` plan.

## Suggested Execution Order
1. Build shared DTO foundations and error enums.
2. Unify global exception handling and security errors.
3. Implement paginated users endpoint + frontend integration.
4. Roll pagination/filtering to remaining list endpoints.
5. Add versioning governance checks and compatibility tests.
6. Add final regression suite and docs updates.
