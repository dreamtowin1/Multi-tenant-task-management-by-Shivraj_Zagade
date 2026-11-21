# Multi-Tenant Task Management System (Single Database)

A SaaS-style multi-tenant task management backend built with **Spring Boot 3**, **Spring Security 6**, **JWT (JJWT)**, **Spring Data JPA**, and a single **H2** database. Multiple organizations (tenants) share the same application and database, while strict tenant isolation is enforced at every layer.

---

## 1. How Tenant Isolation Is Achieved

Tenant isolation is enforced across all five architectural layers:

1. **API / DTO Layer (Zero Client-Supplied `tenant_id`)**:
   - No request DTO (`TenantRegisterRequest`, `CreateUserRequest`, `CreateTaskRequest`, `AssignTaskRequest`, `UpdateTaskStatusRequest`) or endpoint accepts `tenant_id` as a query parameter, path variable, header, or JSON body field.
   - Any `tenant_id` sent by a malicious client is ignored; the tenant is **only** resolved from the verified JWT.
2. **Security Filter & Context Layer (`JwtAuthenticationFilter` + `TenantContext`)**:
   - `JwtAuthenticationFilter` (`OncePerRequestFilter`) validates the `Authorization: Bearer <token>` JWT signature and expiration, extracts `tenant_id`, `role`, `user_id`, and `sub` (email), and stores `tenant_id` in `TenantContext` (`ThreadLocal<String>`) as well as in `TenantPrincipal` inside `SecurityContextHolder`.
   - `TenantContext.clear()` is called unconditionally in a `finally` block after `filterChain.doFilter(...)` to prevent `ThreadLocal` leakage across reused servlet threads.
3. **Service Layer (Ownership & Cross-Tenant Validation)**:
   - `UserService` and `TaskService` read `tenantId` exclusively from `TenantContext.getTenantId()`.
   - Before assigning a task (`PUT /tasks/{id}/assign`), `TaskService` verifies both the task and the target assignee user belong to the current `tenantId`.
   - Before updating a task's status (`PUT /tasks/{id}/status`), `TaskService` verifies the task belongs to the current `tenantId` **and** is assigned to the authenticated `USER` (`task.getAssignedTo().equals(principal.getUserId())`).
   - `GET /tasks` returns all tenant tasks for `ADMIN` and `MANAGER`, and only tasks assigned to the calling `USER` (`findAllByTenantIdAndAssignedTo`).
4. **Repository Layer (Mandatory `tenant_id` Filtering)**:
   - Every `TaskRepository` query (`findAllByTenantId`, `findAllByTenantIdAndAssignedTo`, `findByIdAndTenantId`) and `UserRepository` tenant query (`findAllByTenantId`, `findByIdAndTenantId`, `findByEmailAndTenantId`) explicitly filters by `tenantId`.
   - No repository method returns task or user data across tenants after authentication.
5. **Database Layer (Single H2 Database)**:
   - Uses a single H2 in-memory database (`jdbc:h2:mem:taskdb`) with a single schema.
   - Every table (`tenants`, `users`, `tasks`) contains a non-null `tenant_id` column indexed for fast tenant-scoped queries.

---

## 2. JWT Structure and Claims

Tokens are signed with **HMAC-SHA256 (`HS256`)** using a 256-bit+ Base64-encoded secret (`jwt.secret`).

- **Header**:
  ```json
  {
    "alg": "HS256"
  }
  ```
- **Claims**:
  - `sub`: Authenticated user's email address
  - `user_id`: Numeric ID of the user
  - `tenant_id`: UUID string identifying the user's tenant
  - `role`: User's role (`ADMIN`, `MANAGER`, or `USER`)
  - `iat`: Issued-at epoch timestamp (seconds)
  - `exp`: Expiration epoch timestamp (seconds, default +24 hours)

### Example JWT Payload

```json
{
  "sub": "manager@acme.com",
  "user_id": 2,
  "tenant_id": "7f3c9b1e-4a2d-4c8b-9e12-8b7a6c5d4e3f",
  "role": "MANAGER",
  "iat": 1732124400,
  "exp": 1732210800
}
```

---

## 3. Security Filter Flow

1. Client sends an HTTP request with `Authorization: Bearer <jwt>`.
2. `JwtAuthenticationFilter` (`OncePerRequestFilter`) intercepts the request once per request lifecycle.
3. If the `Authorization` header is missing or does not start with `Bearer `, the filter delegates down the chain without setting `SecurityContext` or `TenantContext` (public endpoints like `/tenant/register` and `/auth/login` proceed; protected endpoints return `401 Unauthorized`).
4. `JwtService` verifies the HMAC-SHA256 signature and expiration timestamp.
5. Claims (`sub`, `user_id`, `tenant_id`, `role`) are extracted from the verified token.
6. `TenantContext.setTenantId(tenantId)` binds the tenant ID to the current request thread.
7. A `TenantPrincipal` with `ROLE_<role>` authority is placed into `SecurityContextHolder`.
8. `SecurityConfig` and `@PreAuthorize` enforce role-based access (`ADMIN`, `MANAGER`, `USER`), and services/repositories enforce tenant isolation using `TenantContext.getTenantId()`.
9. In the `finally` block of `JwtAuthenticationFilter`, `TenantContext.clear()` removes the `ThreadLocal` value before the thread returns to the pool.

---

## 4. API Endpoints

| Method | Endpoint | Allowed Roles | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/tenant/register` | Public | Creates a new tenant and its initial `ADMIN` user |
| `POST` | `/auth/login` | Public | Authenticates user and returns JWT with `tenant_id` and `role` |
| `POST` | `/users` | `ADMIN` | Creates a new user under the admin's `tenant_id` |
| `GET` | `/users` | `ADMIN` | Lists all users in the admin's `tenant_id` |
| `POST` | `/tasks` | `MANAGER` | Creates a task using `tenant_id` from JWT |
| `PUT` | `/tasks/{id}/assign` | `MANAGER` | Assigns a task to a user in the same tenant |
| `PUT` | `/tasks/{id}/status` | `USER` | Updates status of a task assigned to the authenticated user |
| `GET` | `/tasks` | `USER`, `MANAGER`, `ADMIN` | Lists tenant tasks (all tenant tasks for `ADMIN`/`MANAGER`; assigned tasks only for `USER`) |

---

## 5. Steps to Run the Application

### Prerequisites
- **Java 17+**
- **Maven 3.8+**

### Run Tests & Verify Coverage (>= 80% JaCoCo)
```bash
mvn clean verify
```
After running, view the JaCoCo coverage report at:
`target/site/jacoco/index.html`

### Start the Application
```bash
mvn spring-boot:run
```
The server starts on `http://localhost:8080`.
H2 Console is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:taskdb`, Username: `sa`, empty password).

Runtime configuration is defined in `src/main/resources/application.properties`.
It can be overridden with environment variables, for example `JWT_SECRET` and `JWT_EXPIRATION_MS`.

### Quick `curl` Walkthrough

1. **Register a Tenant & Admin**:
   ```bash
   curl -X POST http://localhost:8080/tenant/register \
     -H "Content-Type: application/json" \
     -d '{"tenantName":"Acme Corp","adminEmail":"admin@acme.com","adminPassword":"Password123!"}'
   ```
2. **Login as Admin**:
   ```bash
   curl -X POST http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@acme.com","password":"Password123!"}'
   ```
3. **Create a Manager & a User (using Admin JWT)**:
   ```bash
   curl -X POST http://localhost:8080/users \
     -H "Authorization: Bearer <ADMIN_JWT>" \
     -H "Content-Type: application/json" \
     -d '{"email":"manager@acme.com","password":"Password123!","role":"MANAGER"}'

   curl -X POST http://localhost:8080/users \
     -H "Authorization: Bearer <ADMIN_JWT>" \
     -H "Content-Type: application/json" \
     -d '{"email":"user@acme.com","password":"Password123!","role":"USER"}'
   ```
4. **Create & Assign a Task (using Manager JWT)**:
   ```bash
   curl -X POST http://localhost:8080/tasks \
     -H "Authorization: Bearer <MANAGER_JWT>" \
     -H "Content-Type: application/json" \
     -d '{"title":"Implement JWT Filter","description":"Extract tenant_id from JWT"}'

   curl -X PUT http://localhost:8080/tasks/1/assign \
     -H "Authorization: Bearer <MANAGER_JWT>" \
     -H "Content-Type: application/json" \
     -d '{"userId":3}'
   ```
5. **Update Task Status & View Tasks (using User JWT)**:
   ```bash
   curl -X PUT http://localhost:8080/tasks/1/status \
     -H "Authorization: Bearer <USER_JWT>" \
     -H "Content-Type: application/json" \
     -d '{"status":"IN_PROGRESS"}'

   curl -X GET http://localhost:8080/tasks \
     -H "Authorization: Bearer <USER_JWT>"
   ```
