# QA Bug Report

Project: Auth App
Date: 2026-04-05

## Verification Summary

- Backend build: passed
- Backend tests: passed
- Frontend production build: passed
- Frontend lint: passed

## Findings

### 1. User creation endpoint skips request validation

Severity: High

The backend user creation endpoint accepts `@RequestBody UserDto` without `@Valid`, so the bean validation rules on `UserDto` are not enforced at the controller boundary. This allows invalid payloads to reach the service layer, where only the email field is checked explicitly. In production, that can create incomplete or inconsistent user records and return less useful error feedback to API clients.

Reference:
- [auth-app-Backend/src/main/java/com/validation/auth/backend/controllers/UserController.java](auth-app-Backend/src/main/java/com/validation/auth/backend/controllers/UserController.java#L44)

### 2. Partial self-service user updates can disable the account unintentionally

Severity: High

`UserServiceImpl.updateUser` always applies `existingUser.setEnable(userDto.isEnable())`. Because `UserDto.enable` is a primitive boolean, any partial update request that omits the field will deserialize it as `false` and may disable the account unintentionally. That is a production data-integrity bug for any client that uses the generic update endpoint without sending a full payload.

Reference:
- [auth-app-Backend/src/main/java/com/validation/auth/backend/services/impl/UserServiceImpl.java](auth-app-Backend/src/main/java/com/validation/auth/backend/services/impl/UserServiceImpl.java#L86)

### 3. Signup flow logs registration data to the browser console

Severity: Medium

The signup page calls `console.log(data)`, `console.log(result)`, and `console.log(error)` during form submission. That leaks user registration details into client logs and is not appropriate for production builds. It also adds noise during support investigations and can expose sensitive payloads in shared environments.

Reference:
- [auth-app-frontend/src/pages/Signup.tsx](auth-app-frontend/src/pages/Signup.tsx#L31)

## QA Notes

The automated checks did not surface build or test failures. The issues above came from manual code-path review after running the test/build suite, so they are still valid production-readiness blockers even though the current automated suite is green.