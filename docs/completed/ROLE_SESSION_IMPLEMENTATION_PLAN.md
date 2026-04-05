# Role-Based Admin and Session Management Implementation Plan

## Status
- Backend session APIs: ✅ implemented
- Backend admin user mutation APIs: ✅ implemented
- Frontend admin dashboard controls: ✅ implemented
- Frontend session management UI: ✅ implemented
- Remaining: add focused regression tests for the new endpoints and UI flows

## Objective
Implement advanced admin controls and session/device management in a way that is secure, auditable, and consistent with the current auth architecture.

## Features in Scope
1. Role-Based Admin Panel (Advanced)
   - Enable and disable users
   - Role assignment and role revocation workflow
   - Session revocation UX for admin actions

2. Session & Device Management
   - Active session listing
   - Remote logout from one session
   - Remote logout from all sessions

## Current Baseline
- Admin dashboard exists in the frontend.
- User management endpoints exist in the backend.
- Refresh-token-backed sessions already exist in the backend.
- JWT access tokens and refresh tokens are already part of the auth flow.

## Design Principles
1. Keep admin-only operations server-authorized, not UI-authorized.
2. Never expose refresh token values to the browser.
3. Treat session revocation as a server-side refresh-token state transition.
4. Preserve auditability of admin and user security actions.
5. Keep the implementation incremental so existing login/logout flows remain stable.

## Phase 1: Data Model and API Design
### 1.1 User Status and Role Management
#### Backend tasks
1. Confirm the user entity supports enable/disable state transitions cleanly.
2. Add dedicated service methods for:
   - enable user
   - disable user
   - assign role
   - revoke role
3. Validate that role changes do not create duplicates.
4. Prevent self-lockout scenarios for admins.

#### API shape
1. Add admin endpoints for user lifecycle actions.
2. Keep existing read endpoints read-only.
3. Return a compact response after each admin mutation with the updated user snapshot or success message.

### 1.2 Session Data Shape
#### Backend tasks
1. Define a session summary DTO based on refresh-token metadata.
2. Include fields such as:
   - session id
   - createdAt
   - expiresAt
   - revoked
   - replacedByToken indicator if useful for audit
   - device/browser hints if available
3. Keep the actual refresh token string server-side only.

#### API shape
1. Add endpoint to list current user sessions.
2. Add endpoint to revoke a specific session.
3. Add endpoint to revoke all sessions for the current user.
4. Optionally allow admin-only access to view another user’s sessions.

## Phase 2: Backend Implementation
### 2.1 Admin User Management
#### Tasks
1. Extend the user service with explicit admin mutation methods.
2. Add controller endpoints for enable/disable and role assignment.
3. Apply strict admin authorization to all mutations.
4. Add validation for role payloads and user identifiers.
5. Add audit logging for all admin mutations.

#### Safety rules
1. Admins can manage other users, but should be blocked from accidental self-disabling without confirmation logic.
2. Role changes should be transactional.
3. Role assignment should be idempotent.

### 2.2 Session Storage and Revocation
#### Tasks
1. Reuse the existing refresh-token table as the session source of truth.
2. Add repository/service methods to:
   - list active sessions by user
   - revoke one session by jti
   - revoke all sessions for a user
3. Ensure revocation marks sessions revoked instead of deleting them so history remains available.
4. Verify revoked refresh tokens cannot be used for new access token issuance.
5. Ensure current browser logout clears the cookie and the server session record.

#### Optional improvements
1. Add lightweight device metadata at refresh-token creation time.
2. Populate last-seen data if the auth flow supports it without risking complexity.

## Phase 3: Frontend Implementation
### 3.1 Admin Dashboard Enhancements
#### Tasks
1. Extend the admin dashboard to show user status clearly.
2. Add actions for:
   - enable user
   - disable user
   - assign role
   - revoke role
   - revoke sessions
3. Use confirmation dialogs for destructive actions.
4. Refresh the user list after every mutation.
5. Prevent destructive actions from being triggered accidentally.

### 3.2 Session Management UI
#### Tasks
1. Add a sessions section to the user profile or a dedicated security page.
2. Display active sessions in a table or card list.
3. Show status, creation time, expiry time, and device hints.
4. Add per-session revoke buttons.
5. Add a "logout all devices" action.
6. Show toast feedback and loading state during revocation operations.

### 3.3 UX Rules
1. Keep admin actions visually distinct from user self-service actions.
2. Explain when a session has already expired or been revoked.
3. Keep the UI state aligned with server response after every update.

## Phase 4: Security and Access Control
### Backend security rules
1. Admin-only operations must be enforced by Spring Security, not by frontend checks.
2. Self-session endpoints must only apply to the authenticated user.
3. Admin session views must not leak raw token values.
4. Rate limiting should remain active on auth endpoints.
5. Audit trails should capture who performed the action and when.

### Abuse prevention
1. Do not allow a user to revoke another user’s sessions unless the route is explicitly admin-only.
2. Do not allow role escalation through generic update endpoints.
3. Ensure login refresh rotation continues to work after session revocation.

## Phase 5: Testing Plan
### Backend tests
1. Verify admin can enable and disable users.
2. Verify admin can assign and revoke roles.
3. Verify non-admin users cannot access admin endpoints.
4. Verify session list returns only current user sessions unless admin scope is intended.
5. Verify revoking one session invalidates that refresh token only.
6. Verify revoke-all invalidates all refresh tokens for the user.
7. Verify revoked refresh tokens cannot refresh access tokens.

### Frontend tests
1. Verify admin dashboard renders status and action controls.
2. Verify confirmation dialogs appear for destructive actions.
3. Verify session list loads and updates correctly.
4. Verify revocation action updates UI after success.
5. Verify error handling when a session is already revoked or expired.

## Phase 6: Suggested Implementation Order
1. Add backend session listing and revocation APIs.
2. Add backend admin enable/disable and role mutation APIs.
3. Wire frontend admin dashboard actions.
4. Add session management UI.
5. Add tests for access control and session revocation.
6. Finalize UX polish and confirmation flows.

## Definition of Done
1. Admin can enable/disable users.
2. Admin can assign/revoke roles.
3. Users can see active sessions.
4. Users can revoke one session or all sessions.
5. Session revocation immediately prevents token refresh.
6. All admin and session actions are covered by backend tests.
7. Frontend builds and lint passes after the changes.

## Implementation Notes
- Prefer adding new endpoints and service methods rather than overloading existing ones.
- Reuse the existing refresh-token model for session management where possible.
- Keep DTOs small and explicit.
- Avoid introducing token values into response payloads or logs.
