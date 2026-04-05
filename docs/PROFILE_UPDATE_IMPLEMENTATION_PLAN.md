# Profile Update Feature Implementation Plan

## Objective
Implement a secure, user-friendly profile update feature so authenticated users can update their own profile information without affecting admin-only controls.

Primary outcomes:
1. Users can update editable profile fields from the Profile page.
2. Backend enforces ownership and validation.
3. Frontend updates local auth state after a successful save.
4. Error messages are consistent with the unified API error format.

## Current Baseline (April 2026)
1. Profile page exists with an Edit UI toggle but no functional save flow.
2. Backend currently exposes:
   - `GET /api/v1/users/me` for self-read.
   - `PUT /api/v1/users/{userId}` and `PUT /api/v1/users/{userId}/admin` for updates.
3. Security already separates admin routes from `/users/me` routes.
4. API error schema has been standardized (`ApiErrorResponse`).

## Scope
### In scope
1. Self-profile update API contract (`/api/v1/users/me`).
2. Frontend form handling and save action in profile screen.
3. Validation and understandable error handling.
4. Test coverage for success, validation failure, and authorization boundaries.

### Out of scope
1. Account deletion flow redesign.
2. Media/file upload system for avatars (only URL-based image field in this phase).
3. Admin profile management changes.

## Functional Requirements
1. A logged-in user can edit and save:
   - `name`
   - `image` (URL string)
2. A logged-in user cannot change via self-profile update:
   - `email`
   - `roles`
   - `enabled/enable`
   - `provider`
3. Save action must show clear success and failure feedback.
4. Cancel action must discard unsaved edits.
5. Updated data must be reflected immediately in UI and auth store.

## API Design
## 1. New self-update endpoint
1. Method: `PUT`
2. Path: `/api/v1/users/me`
3. Auth: required (same as existing `/users/me`)
4. Request body (new DTO):
```json
{
  "name": "John Doe",
  "image": "https://example.com/avatar.png"
}
```
5. Response body: updated `UserDto` (consistent with existing read model).

## 2. Validation rules
1. `name`:
   - required
   - min length 2
   - max length 100 (or project-agreed cap)
2. `image`:
   - optional
   - if present, must be valid URL format

## 3. Error handling
1. Return unified `ApiErrorResponse` for validation and runtime failures.
2. User-facing messages should remain understandable, for example:
   - `"Please check the highlighted fields and try again."`
   - `"You do not have permission to perform this action."`

## Backend Implementation Plan
## Phase A: Contract and DTO
1. Create `UserSelfUpdateRequest` DTO with Bean Validation.
2. Add mapping logic from request DTO to service layer input.

## Phase B: Controller and service
1. Add `PUT /api/v1/users/me` in `UserController`.
2. Resolve authenticated user via `Authentication` and update by email/user id internally.
3. Add dedicated service method (recommended):
   - `UserDto updateCurrentUser(String email, UserSelfUpdateRequest request)`
4. Ensure only allowed fields are updated.
5. Keep admin update endpoint behavior unchanged.

## Phase C: Safety and consistency
1. Reject attempts to mutate non-editable fields through self endpoint.
2. Ensure `updatedAt` is refreshed.
3. Return standardized errors from validation/illegal arguments.

## Frontend Implementation Plan
## Phase D: Service layer
1. Add `updateCurrentUserProfile(payload)` in `AuthService.ts` calling `PUT /users/me`.
2. Define a dedicated TS payload type for editable fields only.

## Phase E: Profile page wiring
1. In `Userprofile.tsx`:
   - add local form state for editable values.
   - initialize form from current user on load.
   - implement input change handlers.
2. On Save:
   - call `updateCurrentUserProfile`.
   - update auth store user object with returned data.
   - show success toast.
3. On Cancel:
   - restore form state from current user.
   - exit edit mode.
4. Add loading/disabled state for Save while request is in flight.

## Phase F: UX polish
1. Inline field errors where possible.
2. Keep global toast fallback for unknown errors.
3. Preserve current design style and responsiveness.

## Security and Authorization Rules
1. Self-profile endpoint must update only the authenticated principal.
2. Never trust client-provided user id for self updates.
3. Keep `/users/{userId}/admin` as admin-only mutation path.
4. Do not expose sensitive internals in response messages.

## Testing Plan
## Backend tests
1. Unit test service method updates only allowed fields.
2. Controller/integration test:
   - authenticated user can update own profile.
   - unauthenticated request returns 401.
   - invalid name/image returns 400 with `VALIDATION_FAILED` schema.
3. Regression test to confirm role/email not changed by self endpoint.

## Frontend tests
1. Profile form edit/save happy path.
2. Save disabled during in-flight request.
3. Cancel resets local edits.
4. Validation/API errors render user-friendly feedback.

## Migration and Rollout Strategy
1. Implement endpoint and frontend wiring in one feature branch.
2. Keep existing endpoints unchanged for backward compatibility.
3. Deploy with no contract break to existing admin/dashboard features.

## Acceptance Criteria
1. User can edit and save own name and image from profile page.
2. UI reflects new values immediately after save.
3. Unauthorized/invalid requests return standardized error schema.
4. No privilege escalation path exists through self-profile update.
5. Backend tests and frontend build pass.

## Suggested Execution Order
1. Add backend DTO and service/controller self-update endpoint.
2. Add frontend service method and profile form wiring.
3. Add backend and frontend tests.
4. Run full verification (`mvn test`, frontend build/lint).
