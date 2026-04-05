# OAuth Implementation Analysis - Auth-App

## Executive Summary

The Auth-App currently implements a **basic OAuth2 authentication system** using Spring Security with three OAuth providers (Google, GitHub, LinkedIn). However, the architecture has significant limitations that prevent implementing account linking functionality. The system currently ties one OAuth provider per user and uses email as the unique identifier, making it difficult to support multiple provider connections for a single account.

---

## 1. BACKEND OAUTH IMPLEMENTATION

### 1.1 Technology Stack
- **Framework**: Spring Boot 3.5.8
- **Security**: Spring Security + OAuth2 Client starter
- **Database**: MySQL with Hibernate/JPA
- **Java Version**: 24

### 1.2 OAuth Provider Configuration

Located in: `application-dev.yaml`

**Configured Providers:**

#### Google
```yaml
google:
  client-id: ${GOOGLE_CLIENT_ID}
  client-secret: ${GOOGLE_CLIENT_SECRET}
  redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
  scope:
    - email
    - profile
    - openid
```

#### GitHub
```yaml
github:
  client-id: ${GITHUB_CLIENT_ID}
  client-secret: ${GITHUB_CLIENT_SECRET}
  redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
  scope:
    - user:email
    - read:user
```

#### LinkedIn
```yaml
linkedin:
  client-id: ${LINKEDIN_CLIENT_ID}
  client-secret: ${LINKEDIN_CLIENT_SECRET}
  client-name: LinkedIn
  client-authentication-method: client_secret_post
  authorization-grant-type: authorization_code
  redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
  scope:
    - openid
    - profile
    - email
  provider:
    authorization-uri: https://www.linkedin.com/oauth/v2/authorization
    token-uri: https://www.linkedin.com/oauth/v2/accessToken
    user-info-uri: https://api.linkedin.com/v2/userinfo
    user-name-attribute: sub
```

### 1.3 User Authentication Flow

```
┌─────────────────────────────────────┐
│  Frontend OAuth Button Click        │
│  (Google/GitHub/LinkedIn)           │
│   /oauth2/authorization/{provider}  │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│  Spring Security OAuth2Login Filter │
│  Redirects to Provider's Auth Page  │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│  User Authorizes at Provider        │
│  (Google/GitHub/LinkedIn consent)   │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│  Provider Redirects to Backend      │
│  /login/oauth2/code/{registrationId}│
│  with Authorization Code           │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────┐
│  OAuth2SuccessHandler.onAuthenticationSuccess()
│  1. Extract OAuth2 User Attributes      │
│  2. Identify Provider (Google/etc)      │
│  3. Extract: providerId, email, name,   │
│     picture/avatar_url                  │
└──────────────────┬───────────────────────┘
                   │
                   ▼
┌───────────────────────────────────────────┐
│  UserRepository.findByEmail(email)        │
│  - If EXISTS: Return existing User        │
│  - If NOT EXISTS: Create new User        │
└──────────────────┬────────────────────────┘
                   │
                   ▼
┌───────────────────────────────────────────┐
│  Create JWT Tokens & Refresh Token        │
│  - Generate Access Token (exp: 1 hour)    │
│  - Generate Refresh Token (exp: 24 hours) │
│  - Store Refresh Token in DB              │
│  - Attach Refresh Token as HttpOnly Cookie
└──────────────────┬────────────────────────┘
                   │
                   ▼
┌───────────────────────────────────────────┐
│  Redirect to Frontend Success URL         │
│  ${FRONTEND_SUCCESS_URL}                  │
│  (default: http://localhost:5173/oauth/   │
│   success)                                │
└───────────────────────────────────────────┘
```

### 1.4 Database Entities

#### User Entity
**Location**: `entities/User.java`

```java
@Entity
@Table(name = "user")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;                    // Unique user identifier
    
    @Column(unique = true)
    private String email;               // Unique - PRIMARY LOOKUP KEY
    
    @Column(length = 500)
    private String name;
    
    @Column(length = 500)
    private String password;            // NULL for OAuth users
    
    private String image;
    private boolean enable;
    
    @Enumerated(EnumType.STRING)
    private Provider provider;          // LOCAL, GOOGLE, GITHUB, LINKEDIN
    
    private String providerId;          // Provider's unique ID (e.g., Google "sub")
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles")
    private Set<Role> roles;
    
    private Instant createdAt;
    private Instant updatedAt;
}
```

#### Provider Enum
**Location**: `entities/Provider.java`

```java
public enum Provider {
    LOCAL,      // Email/password registration
    GOOGLE,
    FACEBOOK,   // Configured but not fully implemented
    GITHUB,
    LINKEDIN
}
```

**Key Issue**: Only ONE provider can be stored per user. A user can only have:
- Either LOCAL (email/password) OR
- One OAuth provider (GOOGLE, GITHUB, LINKEDIN, etc.)

#### RefreshToken Entity
**Location**: `entities/RefreshToken.java`

```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String jti;                 // JWT ID for rotation
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;                  // Reference to User
    
    private Instant createdAt;
    private Instant expiresAt;
    private boolean revoked;            // Token rotation/revocation
    
    private String replacedByToken;     // Tracking token rotation chain
}
```

### 1.5 OAuth User Extraction Logic

**Location**: `security/OAuth2SuccessHandler.java`

The handler extracts provider-specific attributes:

**Google**:
```java
String googleId = oAuth2User.getAttributes().getOrDefault("sub", "");
String email = oAuth2User.getAttributes().getOrDefault("email", "");
String name = oAuth2User.getAttributes().getOrDefault("name", "");
String picture = oAuth2User.getAttributes().getOrDefault("picture", "");
```

**GitHub**:
```java
String githubId = oAuth2User.getAttributes().getOrDefault("id", "");
String name = oAuth2User.getAttributes().getOrDefault("login", "");
String avatarUrl = oAuth2User.getAttributes().getOrDefault("avatar_url", "");
String email = oAuth2User.getAttributes().get("email");
// Falls back to: name + "@github.com" if no email provided
```

**LinkedIn**:
```java
String linkedinId = oAuth2User.getAttributes().getOrDefault("sub", "");
String email = oAuth2User.getAttributes().getOrDefault("email", "");
String name = oAuth2User.getAttributes().getOrDefault("name", "");
String picture = oAuth2User.getAttributes().getOrDefault("picture", "");
```

**Critical Logic**:
```java
User newUser = User.builder()
    .email(email)
    .name(name)
    .image(picture)
    .enable(true)
    .provider(Provider.GOOGLE)  // Sets provider
    .providerId(googleId)       // Sets provider's unique ID
    .build();

// Uses EMAIL as the lookup key
user = userRepository.findByEmail(email)
    .orElseGet(() -> userRepository.save(newUser));
```

### 1.6 Security Configuration

**Location**: `config/SecurityConfig.java`

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(sm -> 
            sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorizeRequests ->
            authorizeRequests
                .requestMatchers(AppConstants.AUTH_PUBLIC_URLS).permitAll()
                .anyRequest().authenticated())
        .oauth2Login(oauth2 ->
            oauth2
                .successHandler(successHandler)
                .failureHandler((request, response, exception) ->
                    response.sendRedirect(frontEndFailureUrl)))
        .addFilterBefore(jwtAuthenticationFilter, 
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

**Configuration Details**:
- **Session Management**: STATELESS (no server-side sessions)
- **CORS**: Enabled with configurable frontend URLs
- **OAuth2 Endpoints**: Automatically handled by Spring Security
- **JWT Filter**: Custom filter validates JWT tokens for all requests
- **Success Handler**: Custom `OAuth2SuccessHandler` generates JWT tokens

### 1.7 JWT Token Structure

**Located in**: `security/JwtService.java`

**Access Token Claims**:
```
{
  "jti": "UUID",
  "sub": "user-id (UUID)",
  "iss": "api.validation.com",
  "iat": 1234567890,
  "exp": 1234571490,
  "email": "user@example.com",
  "roles": ["ROLE_USER"],
  "typ": "access"           // Token type marker
}
TTL: 3600 seconds (1 hour)
```

**Refresh Token Claims**:
```
{
  "jti": "refresh-token-id (UUID)",
  "sub": "user-id (UUID)",
  "iss": "api.validation.com",
  "iat": 1234567890,
  "exp": 1234654290,
  "typ": "refresh"          // Token type marker
}
TTL: 86400 seconds (24 hours)
```

### 1.8 API Endpoints

**Authentication Controller**: `controllers/AuthController.java`

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/api/v1/auth/login` | POST | Email/password login | No |
| `/api/v1/auth/refresh` | POST | Refresh access token | No (uses refresh token) |
| `/api/v1/auth/logout` | POST | Logout & revoke tokens | Yes |
| `/api/v1/auth/register` | POST | Email/password registration | No |
| `/api/v1/auth/verify-email` | GET | Verify email token | No |
| `/api/v1/auth/resend-verification` | POST | Resend verification email | No |
| `/api/v1/auth/forgot-password` | POST | Request password reset | No |
| `/api/v1/auth/reset-password` | POST | Reset password with token | No |

**OAuth Endpoints** (Auto-configured by Spring Security):
- `GET /oauth2/authorization/{provider}` - Initiate OAuth flow
- `GET /login/oauth2/code/{provider}` - OAuth callback (internally processed)

### 1.9 Repository Queries

**Location**: `repositores/UserRepository.java`

```java
Optional<User> findByEmail(String email);
boolean existsByEmail(String email);
```

**All OAuth user lookups use email as the primary key.**

---

## 2. FRONTEND OAUTH IMPLEMENTATION

### 2.1 Technology Stack
- **Framework**: React with TypeScript
- **Build Tool**: Vite
- **State Management**: Zustand (with persistence middleware)
- **HTTP Client**: Axios
- **UI Components**: Custom shadcn/ui components

### 2.2 OAuth Button Implementation

**Location**: `components/OAuth2Buttons.tsx`

```typescript
function OAuth2Buttons() {
  return (
    <div className="space-y-3">
      {/* Google Button */}
      <NavLink to={`${import.meta.env.VITE_BASE_URL}/oauth2/authorization/google`}>
        <Button variant="outline" className="w-full">
          <Chrome className="w-5 h-5" /> Continue with Google
        </Button>
      </NavLink>

      {/* GitHub Button */}
      <NavLink to={`${import.meta.env.VITE_BASE_URL}/oauth2/authorization/github`}>
        <Button variant="outline" className="w-full">
          <Github className="w-5 h-5" /> Continue with GitHub
        </Button>
      </NavLink>

      {/* LinkedIn Button */}
      <NavLink to={`${import.meta.env.VITE_BASE_URL}/oauth2/authorization/linkedin`}>
        <Button variant="outline" className="w-full">
          <Linkedin className="w-5 h-5" /> Continue with LinkedIn
        </Button>
      </NavLink>
    </div>
  );
}
```

**Flow**:
1. User clicks OAuth button
2. NavLink redirects to `/oauth2/authorization/{provider}`
3. Browser navigates to backend URL
4. Spring Security handles OAuth flow
5. After user authorization, backend redirects to frontend success/failure page

### 2.3 OAuth Success Page

**Location**: `pages/OAuthSuccess.tsx`

```typescript
function OAuthSuccess() {
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const changeLocalLoginData = useAuth((state) => state.changeLocalLoginData);
  const navigate = useNavigate();

  useEffect(() => {
    async function getAccessToken() {
      if (!isRefreshing) {
        setIsRefreshing(true);
        try {
          // Call refresh endpoint to get access token from refresh token (in cookie)
          const responseLoginData = await refreshToken();
          
          // Update local state with user data
          changeLocalLoginData(
            responseLoginData.accessToken,
            responseLoginData.user,
            true
          );

          toast.success("Login success !");
          navigate("/dashboard");
        } catch (error) {
          toast.error("Error while login!");
          console.log(error);
        } finally {
          setIsRefreshing(false);
        }
      }
    }

    getAccessToken();
  }, []);

  return (
    <div className="p-10 flex flex-col gap-3 justify-center items-center">
      <Spinner />
      <h1 className="text-2xl font-semibold">Please wait....</h1>
    </div>
  );
}
```

**Key Process**:
1. OAuth success page loads
2. Refresh token is already in HttpOnly cookie (from backend)
3. Page calls `/auth/refresh` endpoint
4. Backend validates refresh token, generates new access token
5. Access token returned to frontend
6. User data saved to Zustand store (persisted in localStorage)
7. Redirect to dashboard

### 2.4 OAuth Failure Page

**Location**: `pages/OAuthFailure.tsx`

```typescript
function OAuthFailure() {
  return (
    <div className="p-10 flex justify-center items-center">
      <h1>Login failed!!</h1>
    </div>
  );
}
```

**Limitations**: Basic error page with no error details or recovery options.

### 2.5 Session Management (Zustand Store)

**Location**: `auth/store.ts`

```typescript
type AuthState = {
  accessToken: string | null;
  user: User | null;
  authStatus: boolean;
  authLoading: boolean;
  
  login: (loginData: LoginData) => Promise<LoginResponseData>;
  logout: (silent?: boolean) => void;
  checkLogin: () => boolean | undefined;
  changeLocalLoginData: (
    accessToken: string,
    user: User,
    authStatus: boolean
  ) => void;
};

// Persists auth state to localStorage (key: "app_state")
const useAuth = create<AuthState>()(
  persist(
    (set, get) => ({
      // ... state and actions
    }),
    { name: LOCAL_KEY }
  )
);
```

**Features**:
- Persists auth state to localStorage
- Survives page refreshes
- Single source of truth for auth status

### 2.6 HTTP Client with Token Management

**Location**: `config/ApiClient.ts`

```typescript
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8083/api/v1",
  withCredentials: true,
  timeout: 10000,
});

// Request interceptor: Add access token to every request
apiClient.interceptors.request.use((config) => {
  const accessToken = useAuth.getState().accessToken;
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// Response interceptor: Handle 401 responses with token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const is401 = error.response.status === 401;
    const original = error.config;
    
    if (!is401 || original._retry) {
      return Promise.reject(error);
    }

    original._retry = true;

    if (isRefreshing) {
      // Queue requests while refreshing
      return new Promise((resolve, reject) => {
        queueRequest((newToken: string) => {
          if (!newToken) return reject();
          original.headers.Authorization = `Bearer ${newToken}`;
          resolve(apiClient(original));
        });
      });
    }

    // Start token refresh
    isRefreshing = true;
    try {
      const loginResponse = await refreshToken();
      const newToken = loginResponse.accessToken;
      
      // Update store with new token
      useAuth.getState().changeLocalLoginData(
        loginResponse.accessToken,
        loginResponse.user,
        true
      );
      
      // Retry original request with new token
      resolveQueue(newToken);
      original.headers.Authorization = `Bearer ${newToken}`;
      return apiClient(original);
    } catch (error) {
      resolveQueue("null");
      useAuth.getState().logout();
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }
  }
);
```

**Features**:
- Automatic token injection in requests
- Token refresh on 401 response
- Request queuing during refresh to prevent race conditions
- Automatic logout on refresh token expiration

### 2.7 User Data Models

**Location**: `models/User.ts`

```typescript
interface User {
  id: string;
  email: string;
  name?: string;
  enabled: boolean;
  image?: string;
  updatedAt?: string;
  createdAt?: string;
  provider: string;  // "LOCAL", "GOOGLE", "GITHUB", "LINKEDIN"
}
```

**Location**: `models/LoginResponseData.ts`

```typescript
interface LoginResponseData {
  accessToken: string;
  user: User;
  refreshToken: string;
  expiresIn: number;
}
```

### 2.8 Auth Service

**Location**: `services/AuthService.ts`

```typescript
export const loginUser = async (loginData: LoginData) => {
  const response = await apiClient.post<LoginResponseData>(
    "/auth/login",
    loginData
  );
  return response.data;
};

export const refreshToken = async () => {
  const response = await apiClient.post<LoginResponseData>(`/auth/refresh`);
  return response.data;
};

export const logoutUser = async () => {
  const response = await apiClient.post(`/auth/logout`);
  return response.data;
};

export const getCurrentUser = async (emailId: string | undefined) => {
  const response = await apiClient.get<User>(`/users/email/${emailId}`);
  return response.data;
};
```

---

## 3. CURRENT ARCHITECTURE DIAGRAM

```
┌─── BACKEND (Spring Boot) ────────────────────────────────┐
│                                                            │
│  ┌────────────────────────────────────┐                  │
│  │  OAuth2SuccessHandler              │                  │
│  │  ├─ Extract Provider Attributes    │                  │
│  │  ├─ Match: GOOGLE/GITHUB/LINKEDIN  │                  │
│  │  └─ Extract: providerId, email     │                  │
│  └────────────────┬───────────────────┘                  │
│                   │                                       │
│                   ▼                                       │
│  ┌────────────────────────────────────┐                  │
│  │  UserRepository                    │                  │
│  │  └─ findByEmail(email)             │      ◄── Only lookup key
│  │     ├─ EXISTS → Return User        │                  │
│  │     └─ NOT EXISTS → Create New     │                  │
│  └────────────────┬───────────────────┘                  │
│                   │                                       │
│                   ▼                                       │
│  ┌────────────────────────────────────┐                  │
│  │  User Entity                       │                  │
│  │  └─ id, email, provider (SINGLE),  │                  │
│  │     providerId, name, image, roles │                  │
│  └────────────────┬───────────────────┘                  │
│                   │                                       │
│                   ▼                                       │
│  ┌────────────────────────────────────┐                  │
│  │  JwtService                        │                  │
│  │  ├─ Generate Access Token (1h)     │                  │
│  │  └─ Generate Refresh Token (24h)   │                  │
│  └────────────────┬───────────────────┘                  │
│                   │                                       │
│                   ▼                                       │
│  ┌────────────────────────────────────┐                  │
│  │  RefreshTokenRepository            │                  │
│  │  └─ Store Refresh Token + JTI      │                  │
│  └────────────────┬───────────────────┘                  │
│                   ▼                                       │
│        Redirect to Frontend                              │
│        + Set RefreshToken HttpOnly Cookie               │
└─────────────────────────────────────────────────────────┘

┌─── DATABASE ─────────────────────────────────────────────┐
│                                                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   users      │  │ refresh_    │  │    roles      │  │
│  │──────────────│  │ tokens       │  │──────────────│  │
│  │ id (PK)      │  │──────────────│  │ id (PK)     │  │
│  │ email (UQ)  │  │ id (PK)     │  │ name        │  │
│  │ name         │  │ jti (UQ)    │  └──────────────┘  │
│  │ password     │  │ user_id (FK)│                    │
│  │ image        │  │ createdAt   │  ┌──────────────┐  │
│  │ provider     │  │ expiresAt   │  │  user_roles  │  │
│  │ providerId   │  │ revoked     │  │──────────────│  │
│  │ enable       │  │ replacedBy­Token
│  │              │  │             │  │ user_id (FK) │  │
│  └──────────────┘  └─────────────┘  │ roles_id (FK)│  │
│                                       └──────────────┘  │
└──────────────────────────────────────────────────────────┘

┌─── FRONTEND (React) ─────────────────────────────────────┐
│                                                            │
│  ┌──────────────────────────┐                            │
│  │  OAuth2Buttons Component │                            │
│  │  ├─ Google Button        │                            │
│  │  ├─ GitHub Button        │                            │
│  │  └─ LinkedIn Button      │                            │
│  └──────────────┬───────────┘                            │
│                 │ redirects to                            │
│                 ▼                                         │
│      /oauth2/authorization/{provider}                     │
│                 │                                         │
│      After authorization:                                │
│                 ▼                                         │
│  ┌────────────────────────────────────┐                  │
│  │  OAuthSuccess Page                 │                  │
│  │  ├─ Call /auth/refresh endpoint    │                  │
│  │  ├─ Update Zustand store           │                  │
│  │  └─ Redirect to /dashboard         │                  │
│  └────────────────┬───────────────────┘                  │
│                   │                                       │
│                   ▼                                       │
│  ┌────────────────────────────────────┐                  │
│  │  Zustand Auth Store (localStorage) │                  │
│  │  ├─ accessToken                    │                  │
│  │  ├─ user { id, email, provider }   │                  │
│  │  ├─ authStatus (boolean)           │                  │
│  │  └─ login/logout methods           │                  │
│  └────────────────┬───────────────────┘                  │
│                   │                                       │
│                   ▼                                       │
│  ┌────────────────────────────────────┐                  │
│  │  ApiClient (Axios)                 │                  │
│  │  ├─ Request Interceptor: Add JWT   │                  │
│  │  ├─ Response Interceptor: Refresh  │                  │
│  │  │  on 401 + retry request         │                  │
│  │  └─ Automatic token rotation       │                  │
│  └────────────────────────────────────┘                  │
└──────────────────────────────────────────────────────────┘
```

---

## 4. CURRENT LIMITATIONS PREVENTING ACCOUNT LINKING

### 4.1 Core Architecture Limitations

#### **Limitation #1: Single Provider Per User**
- **Problem**: `User.provider` field is a single enum value
- **Impact**: Can only store ONE provider type per user
- **Example Scenario**:
  - User logs in with Google → `provider = GOOGLE`
  - Same user tries to log in with GitHub → `provider` would need to change to GITHUB
  - **Result**: Can't maintain both simultaneously

#### **Limitation #2: Email-Based User Lookup**
- **Problem**: `UserRepository.findByEmail()` is the ONLY lookup mechanism
- **Current Logic**:
  ```java
  User user = userRepository.findByEmail(email)
      .orElseGet(() -> userRepository.save(newUser));
  ```
- **Impact**:
  - If user@gmail.com signs up with Google
  - And user@gmail.com later tries GitHub with same email
  - **Result**: They get the SAME User object, overwriting provider
  - No way to link/unlink - provider gets overwritten

#### **Limitation #3: No Provider Linking Relationship**
- **Current State**: 
  - User has 1:1 relationship with provider
  - No intermediate table for user-provider mappings
- **Missing Schema**: Would need something like:
  ```
  user_provider_links
  ├─ id (PK)
  ├─ user_id (FK)
  ├─ provider (ENUM)
  ├─ provider_id (provider's unique ID)
  ├─ provider_email
  ├─ created_at
  └─ is_primary (boolean)
  ```

#### **Limitation #4: No Provider Identity Storage**
- **Current**: `providerId` field stores provider's unique ID, but only for ONE provider
- **Problem**: 
  - Overwritten when user logs in with different provider
  - No history of linked providers
  - No metadata about each provider (access token, scope, etc.)

#### **Limitation #5: No User Authentication State Tracking**
- **Current**: Can't distinguish between:
  - User authenticating with LOCAL (email/password)
  - User authenticating with GOOGLE
  - User authenticating with GITHUB
- **Problem**: When user@gmail.com exists in system:
  - Can they still use password login?
  - What if they registered with password first, then Google?
  - No tracking of which auth methods are available per user

#### **Limitation #6: No Account Linking API Endpoints**
- **Currently Missing**:
  - `POST /api/v1/auth/link-provider` - Link new provider
  - `DELETE /api/v1/auth/providers/{provider}` - Unlink provider
  - `GET /api/v1/auth/linked-providers` - List linked providers
  - `PUT /api/v1/auth/set-primary-provider` - Change primary provider
  - `POST /api/v1/auth/verify-provider-email` - Verify linked email

#### **Limitation #7: No Frontend UI for Account Linking**
- **Currently Missing**:
  - Account settings page showing linked providers
  - "Link provider" buttons
  - "Unlink provider" confirmation dialog
  - Option to change primary provider
  - Security verification for linking/unlinking

#### **Limitation #8: No Conflict Resolution**
- **Scenario**: User A has `email: user@example.com` via Google
- **Scenario**: User B tries to login with GitHub using same `email: user@example.com`
- **Current Behavior**: One overwrites the other (undefined behavior)
- **Needed**: 
  - Email verification before linking
  - Conflict detection
  - User-initiated provider linking flow

#### **Limitation #9: No Security Controls**
- **Missing**:
  - Two-factor authentication for linking
  - Email verification for linked providers
  - Session validation before unlinking
  - Audit log of provider link/unlink events
  - Password requirement if user wants to keep LOCAL auth

---

## 5. WHAT NEEDS TO BE ADDED FOR ACCOUNT LINKING

### 5.1 Database Schema Changes

#### New Entity: `ProviderLink`
```java
@Entity
@Table(name = "user_provider_links", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "provider"})
       })
public class ProviderLink {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;
    
    @Column(nullable = false, unique = true)
    private String providerId;  // Provider's unique ID (e.g., Google "sub")
    
    @Column(unique = true)
    private String providerEmail;  // Email from provider
    
    private String displayName;    // Name from provider
    private String profileImage;   // Avatar URL from provider
    
    @Column(columnDefinition = "JSON")
    private String providerMetadata;  // Store access token, scopes, etc as JSON
    
    @Column(nullable = false)
    private boolean isPrimary;     // Which provider to use for name/image
    
    private Instant createdAt;
    private Instant linkedAt;      // When this provider was linked
    private Instant lastUsedAt;    // Track which providers are actively used
    
    @Version
    private Long version;          // Optimistic locking for concurrent updates
}
```

#### Modified Entity: `User`
```java
// REMOVE:
private Provider provider;
private String providerId;

// ADD:
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
private Set<ProviderLink> providerLinks = new HashSet<>();

// ADD method to get primary provider:
public ProviderLink getPrimaryProviderLink() {
    return providerLinks.stream()
        .filter(ProviderLink::isPrimary)
        .findFirst()
        .orElse(null);
}

// ADD method to check if provider linked:
public boolean isProviderLinked(Provider provider) {
    return providerLinks.stream()
        .anyMatch(link -> link.getProvider() == provider);
}
```

#### New Entity: `ProviderLinkToken` (for email verification)
```java
@Entity
@Table(name = "provider_link_tokens")
public class ProviderLinkToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, unique = true)
    private String token;  // Secure random token
    
    @Enumerated(EnumType.STRING)
    private Provider provider;
    
    private String providerCode;  // OAuth code to validate
    private String providerEmail;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant expiresAt;
    
    private boolean used;
}
```

### 5.2 New API Endpoints Needed

```
# Get linked providers
GET /api/v1/auth/providers
Response: List<{provider, providerEmail, linkedAt, isPrimary}>

# Initiate linking new provider
GET /api/v1/auth/link-provider/{provider}
QueryParam: ?returnUrl=/dashboard
Action: OAuth login flow, but marked as "linking" not "login"

# Verify provider link after OAuth callback
POST /api/v1/auth/verify-provider-link
Body: {provider, authorizationCode, state}
Response: {success, message}

# Get linking status (for WebSocket updates)
GET /api/v1/auth/linking-status

# Unlink provider
DELETE /api/v1/auth/providers/{provider}
Security: Requires password if it's the last auth method + MFA

# Set primary provider
PUT /api/v1/auth/providers/{provider}/set-primary
Response: {updatedProviders}

# Get available link options
GET /api/v1/auth/available-providers
Response: List of providers for which user can authenticate
```

### 5.3 New Services/Components Needed

#### Backend Service: `ProviderLinkingService`
- Validate provider linking request
- Generate secure linking tokens
- Verify that new provider email doesn't conflict
- Handle provider unlinking
- Migrate provider metadata

#### Backend Service: `ProviderConflictResolver`
- Handle case where two users claim same email
- Send verification emails
- Allow user to choose linking vs account separation

#### Backend Service: `ProviderMetadataManager`
- Store OAuth access tokens securely
- Refresh access tokens when needed
- Handle scope changes
- Audit provider access changes

#### Frontend Component: `LinkedProvidersManagement`
- Display all linked providers
- Show link/unlink buttons
- Indicate primary provider
- Confirmation dialogs for unlinking

#### Frontend Component: `LinkProviderFlow`
- OAuth button for linking (different from login)
- Email verification if needed
- Conflict resolution UI
- Success message with next steps

### 5.4 New API Response Types

```typescript
// Backend
interface ProviderLinkResponse {
  provider: string;
  providerEmail: string;
  displayName?: string;
  profileImage?: string;
  linkedAt: Date;
  lastUsedAt?: Date;
  isPrimary: boolean;
}

interface UserWithProvidersResponse extends UserDto {
  linkedProviders: ProviderLinkResponse[];
  primaryProvider: Provider;
  canRemoveProvider: boolean;  // False if last auth method
}

// Frontend
interface LinkedProvider {
  provider: "GOOGLE" | "GITHUB" | "LINKEDIN" | "LOCAL";
  email?: string;
  linkedAt: Date;
  lastUsedAt?: Date;
  isPrimary: boolean;
  canUnlink: boolean;
}

interface AccountSettingsState {
  linkedProviders: LinkedProvider[];
  primaryProvider: Provider;
  isLinkingInProgress: boolean;
  linkingProvider?: Provider;
}
```

### 5.5 Security Considerations

- **Token Revocation**: When unlinking, revoke provider's access tokens
- **Email Verification**: Before allowing linking, verify email ownership
- **Rate Limiting**: Limit linking attempts per user per hour
- **Session Validation**: Require recent authentication before unlinking
- **Password Enforcement**: If provider is unlinked, user needs password
- **MFA**: Require MFA verification for sensitive linking operations
- **Audit Trail**: Log all linking/unlinking events with timestamp and source IP
- **Concurrent Access**: Use optimistic locking on User entity

---

## 6. SUMMARY TABLE: CURRENT vs. REQUIRED ARCHITECTURE

| Aspect | Current State | Required State |
|--------|---------------|----------------|
| **User-Provider Relationship** | 1:1 (single) | 1:N (multiple) |
| **Provider Storage** | User.provider (enum) | Separate ProviderLink entity |
| **Provider Lookups** | Email only | Email + providerId mapping |
| **Linking UI** | None | Account settings page |
| **Linking Endpoints** | None | Link/unlink/set-primary APIs |
| **Email Verification** | Not used for OAuth | Required for provider linking |
| **Conflict Resolution** | None (overwrites) | User-initiated, verified |
| **Audit Trail** | Not available | Full logging |
| **Security Controls** | Basic JWT | MFA, password verification |
| **Primary Provider** | Implicit | Explicitly tracked |
| **Provider Metadata** | Only providerId | Full OAuth tokens + metadata |
| **Access Token Management** | Per-session only | Store & refresh provider tokens |

---

## 7. MIGRATION PATH RECOMMENDATIONS

### Phase 1: Schema Preparation (0-1 weeks)
1. Create `user_provider_links` table with migration
2. Create `provider_link_tokens` table
3. Add foreign key relationships
4. Backfill existing users into `user_provider_links`

### Phase 2: Backend Implementation (1-2 weeks)
1. Create `ProviderLink` entity with proper relationships
2. Implement `ProviderLinkingService` with core logic
3. Implement linking/unlinking endpoints
4. Add comprehensive validation and conflict resolution
5. Implement token refresh mechanism for provider access tokens

### Phase 3: Frontend Implementation (1-2 weeks)
1. Create account settings page with provider management
2. Add "Link Provider" flow with OAuth integration
3. Add "Unlink Provider" confirmation dialog
4. Update Zustand store to track linked providers
5. Add provider linking status UI

### Phase 4: Testing & Hardening (1 week)
1. End-to-end testing of all linking scenarios
2. Security testing for conflict resolution
3. Performance testing for multi-provider lookups
4. Load testing for concurrent linking operations

---

## 8. ENVIRONMENTAL CONFIGURATION

All OAuth credentials configured via environment variables:

**Development (.env.local or application-dev.yaml)**
```
GOOGLE_CLIENT_ID=xxx
GOOGLE_CLIENT_SECRET=xxx
GITHUB_CLIENT_ID=xxx
GITHUB_CLIENT_SECRET=xxx
LINKEDIN_CLIENT_ID=xxx
LINKEDIN_CLIENT_SECRET=xxx

FRONTEND_URL=http://localhost:5173
FRONTEND_SUCCESS_URL=http://localhost:5173/oauth/success
FRONTEND_FAILURE_URL=http://localhost:5173/oauth/failure
```

**Frontend (.env.local)**
```
VITE_API_BASE_URL=http://localhost:8083/api/v1
VITE_BASE_URL=http://localhost:8083
```

---

## 9. TESTING SCENARIOS

### Current System Test Cases
- ✅ Login with email/password
- ✅ Google OAuth login
- ✅ GitHub OAuth login
- ✅ LinkedIn OAuth login
- ✅ Token refresh mechanism
- ✅ Token revocation on logout
- ✅ Access token expiration handling

### Required Test Cases for Account Linking
- Link provider to existing EMAIL/password account
- Link provider to existing OAuth account (different provider)
- Attempt to link provider with conflicting email
- Unlink provider (with single provider remaining)
- Change primary provider
- Provider access token refresh during linking
- Concurrent linking attempts
- Rate limiting of linking attempts
- Session timeout during linking flow
- Email verification during linking

---

## 10. KEY FILES REFERENCE

### Backend
- OAuth Config: [application-dev.yaml](../auth-app-Backend/src/main/resources/application-dev.yaml#L30)
- OAuth Handler: [OAuth2SuccessHandler.java](../auth-app-Backend/src/main/java/com/validation/auth/backend/security/OAuth2SuccessHandler.java)
- User Entity: [User.java](../auth-app-Backend/src/main/java/com/validation/auth/backend/entities/User.java)
- Auth Controller: [AuthController.java](../auth-app-Backend/src/main/java/com/validation/auth/backend/controllers/AuthController.java)
- Security Config: [SecurityConfig.java](../auth-app-Backend/src/main/java/com/validation/auth/backend/config/SecurityConfig.java)
- JWT Service: [JwtService.java](../auth-app-Backend/src/main/java/com/validation/auth/backend/security/JwtService.java)

### Frontend
- OAuth Buttons: [OAuth2Buttons.tsx](../auth-app-frontend/src/components/OAuth2Buttons.tsx)
- OAuth Success: [OAuthSuccess.tsx](../auth-app-frontend/src/pages/OAuthSuccess.tsx)
- Auth Store: [store.ts](../auth-app-frontend/src/auth/store.ts)
- API Client: [ApiClient.ts](../auth-app-frontend/src/config/ApiClient.ts)
- Auth Service: [AuthService.ts](../auth-app-frontend/src/services/AuthService.ts)

