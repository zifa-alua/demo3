# BITLAB News API — TASK-003 (JWT Authentication)

Backend REST API for a news service, built on **Spring Boot + PostgreSQL** with
full **JWT authentication**, role-based access control, refresh-token rotation
and basic brute-force protection.

This is a continuation of TASK-002 (news CRUD): TASK-003 adds the security layer.

---

## Tech stack

- Java 17
- Spring Boot 4 (Spring Web, Spring Security 6, Spring Data JPA, Validation)
- PostgreSQL 16 (in Docker)
- jjwt (io.jsonwebtoken) for JWT
- Maven

---

## Prerequisites

- **JDK 17**
- **Docker Desktop** (for the PostgreSQL container)
- **IntelliJ IDEA** (or any IDE / Maven)

---

## 1. Start the database (PostgreSQL in Docker)

The app expects PostgreSQL on **port 5433**, database **`bitlab_news`**.

**Option A — docker-compose (recommended).** A `docker-compose.yml` is included
in the project root. From the project folder run:

```bash
docker compose up -d
```

**Option B — plain docker run:**

```bash
docker run --name bitlab-postgres ^
  -e POSTGRES_USER=news_user ^
  -e POSTGRES_PASSWORD=localpass123 ^
  -e POSTGRES_DB=bitlab_news ^
  -p 5433:5432 ^
  -d postgres:16
```

> On macOS/Linux replace the `^` line-continuations with `\`.

Check it is running: `docker ps` should list `bitlab-postgres`.

---

## 2. Environment variables

Secrets are **never** stored in the code or in `application.yml` — they are read
from environment variables. Set these before running the app
(in IntelliJ: **Run → Edit Configurations → Environment variables**):

| Variable      | Example value                                   | Description                          |
|---------------|-------------------------------------------------|--------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5433/bitlab_news`  | JDBC URL to the database             |
| `DB_USER`     | `news_user`                                     | Database user                        |
| `DB_PASSWORD` | `localpass123`                                  | Database password                    |
| `JWT_SECRET`  | *(64-byte Base64 string — generate your own)*   | Secret key used to sign JWT tokens   |

**Generate a JWT secret** (PowerShell):

```powershell
$b = New-Object byte[] 64; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

> The app **will not start** without `JWT_SECRET` — this is intentional, so a
> weak or empty secret can never be used by accident.

---

## 3. Run the application

In IntelliJ press **Run** (▶️), or from the terminal:

```bash
./mvnw spring-boot:run
```

The app starts on **http://localhost:8080**. On the first run the tables
(`users`, `refresh_tokens`, `news`) are created automatically.

---

## 4. API endpoints

### Auth (public — no token required)

| Method | Endpoint         | Body                          | Success |
|--------|------------------|-------------------------------|---------|
| POST   | `/auth/register` | `email`, `password`           | 201     |
| POST   | `/auth/login`    | `email`, `password`           | 200 → `accessToken`, `refreshToken` |
| POST   | `/auth/refresh`  | `refreshToken`                | 200 → new token pair |
| POST   | `/auth/logout`   | `refreshToken`                | 200 (token revoked) |

### News (protected — Bearer token required)

| Method | Endpoint      | Who can call   | Success |
|--------|---------------|----------------|---------|
| GET    | `/api/news`   | any logged-in user | 200 |
| POST   | `/api/news`   | **ADMIN only** | 201 |
| PUT    | `/api/news/{id}` | **ADMIN only** | 200 |
| DELETE | `/api/news/{id}` | **ADMIN only** | 200/204 |

Send the access token in the header:

```
Authorization: Bearer <accessToken>
```

---

## 5. Roles

- **STUDENT** — default role for every newly registered user. Can only read news (GET).
- **ADMIN** — can create/update/delete news.

Registration always assigns **STUDENT** (the client cannot choose its own role —
this prevents privilege escalation). To grant ADMIN, update the database
directly:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'someone@test.kz';
```

Then log in again to receive a token carrying the new role.

---

## 6. Testing

A ready-to-run request collection is in **`requests.http`** (open it in IntelliJ
and click the green arrow next to each request). Recommended order and expected
status codes:

| # | Request                         | Condition                       | Expected |
|---|---------------------------------|---------------------------------|----------|
| 1 | POST `/auth/register`           | new email                       | 201 (409 if already exists) |
| 2 | POST `/auth/login`              | correct password                | 200      |
| 3 | GET `/api/news`                 | valid access token              | 200      |
| 4 | GET `/api/news`                 | no token                        | 401      |
| 5 | POST `/api/news`                | STUDENT token                   | 403      |
| 5'| POST `/api/news`                | ADMIN token, all fields         | 201      |
| 6 | POST `/auth/refresh`            | valid refresh token             | 200      |
| 7 | POST `/auth/logout`             | refresh token                   | 200      |
| 8 | POST `/auth/refresh`            | revoked refresh token           | 401      |

Access tokens expire after **15 minutes**; refresh tokens after **7 days** and
are single-use (rotated on every refresh, revoked on logout).

---

## 7. Security measures implemented

- **Passwords** are stored only as **BCrypt** hashes — never plain text.
- **JWT secret** is provided only via the `JWT_SECRET` environment variable,
  never committed to the repository.
- **Algorithm `none` is rejected** — tokens are verified with an explicit HS256
  signing key; an unsigned token fails verification and returns 401.
- **Refresh tokens** are stored in the database as **SHA-256 hashes**, not in
  plain form; they are rotated on refresh and revoked on logout.
- **Role from the database**, not from the token payload — the role claim is
  never trusted blindly.
- **Brute-force protection** — login is rate-limited (5 attempts per minute per
  client), returning **429** when exceeded.
- **Invalid / missing token → 401** (without leaking why); **wrong role → 403**.
- Registration forces the **STUDENT** role to prevent privilege escalation.

---

## Project structure

```
src/main/java/com/example/demo/
├── controller/      # AuthController, NewsController
├── dto/             # request/response DTOs
├── entity/          # User, Role, RefreshToken, News
├── exception/       # custom exceptions + GlobalExceptionHandler
├── repository/      # Spring Data JPA repositories
├── security/        # JwtUtil, JwtAuthenticationFilter, SecurityConfig,
│                    # PasswordConfig, TokenHashUtil, LoginRateLimiter,
│                    # RestAuthEntryPoint
└── service/         # AuthService, NewsService
```
