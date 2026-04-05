# Auth App Frontend

React, TypeScript, and Vite frontend for the Auth App. It provides login, registration, OAuth2 sign-in, email verification, password recovery, user dashboard pages, admin tools, and active session management.

## Features

- Email/password login and registration
- OAuth2 sign-in with Google, GitHub, and LinkedIn
- Email verification, resend verification, forgot password, and reset password flows
- Protected dashboard and admin routes
- User profile with current session listing and remote logout controls
- Admin dashboard for create, update, delete, and enable/disable user workflows
- Theme provider and responsive UI styling
- Toast-based error and success feedback

## Tech stack

- React 19
- TypeScript 5.9
- Vite 7
- React Router 7
- Axios
- Zustand
- Tailwind CSS 4
- Radix UI primitives
- Framer Motion
- Lucide React
- react-hot-toast
- react-day-picker

## Prerequisites

- Node.js 16 or newer
- npm
- Backend running at `http://localhost:8083`

## Environment setup

Create a `.env` file in the frontend root:

```env
VITE_API_BASE_URL=http://localhost:8083/api/v1
VITE_BASE_URL=http://localhost:8083
```

`VITE_API_BASE_URL` is used for API calls, while `VITE_BASE_URL` is used for OAuth2 redirects.

## Install and run

```bash
npm install
npm run dev
```

For a production build:

```bash
npm run build
```

To preview the production build locally:

```bash
npm run preview
```

To run lint checks:

```bash
npm run lint
```

## Routes

| Route | Purpose |
| --- | --- |
| `/` | Landing page |
| `/login` | Email/password login |
| `/signup` | Account registration |
| `/forgot-password` | Start password recovery |
| `/reset-password` | Finish password reset |
| `/verify-email` | Email verification page |
| `/services` | Marketing/services page |
| `/about` | About page |
| `/dashboard` | Protected user dashboard |
| `/dashboard/profile` | User profile and session controls |
| `/admin` | Protected admin dashboard |
| `/oauth/success` | OAuth success callback |
| `/oauth/failure` | OAuth failure callback |

## Authentication flow

### Login and refresh

1. The login form posts to `/auth/login`.
2. The backend returns an access token and user payload.
3. The frontend keeps the access token in memory and persists only user/auth state.
4. `RootLayout` attempts a refresh on load through `/auth/refresh`.
5. API requests retry after a refresh when a 401 is received.

### Registration and verification

1. The signup form posts to `/auth/register`.
2. The app redirects to `/verify-email` with the registered email.
3. The verification page can call the backend verification endpoint and resend flow.

### Password recovery

1. Users can start recovery from `/forgot-password`.
2. They complete recovery from `/reset-password` using the token from email.

### Session management

1. The profile page loads `/users/me/sessions`.
2. Users can revoke a single session or revoke all sessions.
3. The admin dashboard can manage user access and session state through admin endpoints.

## API calls used by the frontend

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/auth/register` | Register a user |
| POST | `/auth/login` | Login |
| POST | `/auth/logout` | Logout |
| POST | `/auth/refresh` | Refresh the session |
| GET | `/auth/verify-email` | Verify email token |
| POST | `/auth/resend-verification` | Resend verification email |
| POST | `/auth/forgot-password` | Start password recovery |
| POST | `/auth/reset-password` | Reset password |
| GET | `/users/me` | Get current user |
| GET | `/users/me/sessions` | List current sessions |
| DELETE | `/users/me/sessions` | Revoke all current sessions |
| DELETE | `/users/me/sessions/{jti}` | Revoke one current session |
| GET | `/users` | Admin user list |
| POST | `/users` | Admin create user |
| PUT | `/users/{userId}/admin` | Admin update user access |
| GET | `/users/roles` | Load role names |
| GET | `/users/{userId}/sessions` | Admin session list |
| DELETE | `/users/{userId}/sessions` | Admin revoke all sessions for a user |
| DELETE | `/users/{userId}/sessions/{jti}` | Admin revoke one session for a user |

## Notes

- The app is designed to work with the backend defaults and route structure in the backend README.
- The access token is not persisted to browser storage.
- The UI uses protected routes for user and admin areas.
