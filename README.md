# Auth App

Full-stack authentication and authorization app built with Spring Boot on the backend and React on the frontend. It supports email/password auth, OAuth2 login, email verification, password recovery, role-based admin controls, and active session management.

## Repo layout

- `auth-app-Backend/` - Spring Boot API and security layer
- `auth-app-frontend/` - React user interface

## Current capabilities

- JWT login, refresh, and logout flows
- OAuth2 sign-in with Google, GitHub, and LinkedIn
- Email verification and resend flow
- Forgot password and reset password flow
- User profile, dashboard, and admin management screens
- Active session listing and remote session revocation
- Role-based access control and request validation
- Rate limiting and security hardening on auth endpoints

## Run locally

### Backend

```bash
cd auth-app-Backend
copy .env.example .env
./mvnw spring-boot:run
```

Backend default URL: `http://localhost:8083`

Swagger UI: `http://localhost:8083/swagger-ui.html`

### Frontend

```bash
cd auth-app-frontend
npm install
npm run dev
```

Frontend default URL: `http://localhost:5173`

## Configuration

- Backend environment variables are documented in `auth-app-Backend/README.md`.
- Frontend environment variables are documented in `auth-app-frontend/README.md`.

## Notes

- The frontend refreshes the session on load using the backend refresh endpoint.
- Access tokens are not persisted in browser storage; user state and auth status are restored separately.
- The backend exposes profile, admin, and session management endpoints under `/api/v1`.
