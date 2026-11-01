# Clinic App

Full-stack clinic management app with a Spring Boot REST API (`backend/`) and a React + Vite frontend (`frontend/`).

- Java 17 / Spring Boot 3
- PostgreSQL, Spring Data JPA, Flyway migrations
- Spring Security and JWT authentication
- React + Vite frontend

## Before running

1. Install Java 17, Node.js, and PostgreSQL.
2. Create a PostgreSQL database named `clinic_db`.
3. Copy `.env.example` to `.env` and set real values for `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, and `ADMIN_PASSWORD`.
4. Export those values into the environment used to launch Spring Boot. Spring Boot does not automatically load a `.env` file by itself.
5. Use a randomly generated JWT secret with at least 32 bytes of key material. For example, generate one with `openssl rand -base64 48` and store the generated string as `JWT_SECRET`.

The first doctor/admin account is provisioned from `ADMIN_EMAIL` and `ADMIN_PASSWORD` if no admin exists. Public registration always creates a patient.

## Run locally

### Development (two terminals)

Backend:

```bash
cd backend
./mvnw spring-boot:run
# Windows: mvnw.cmd spring-boot:run
```

Frontend:

```bash
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`.

### Serve the built frontend from Spring Boot

```bash
cd frontend
npm ci
npm run build

cd ../backend
./mvnw clean package
./mvnw spring-boot:run
```

Open `http://localhost:8080`. Swagger is disabled by default; enable it only for development with `SWAGGER_ENABLED=true`.

## Database migrations and existing databases

Flyway owns schema changes. Hibernate is configured to validate the schema rather than silently update it (`spring.jpa.hibernate.ddl-auto=validate`). Do not edit a migration after it has been applied to a shared or existing database; create a new versioned migration instead.

- A fresh empty database runs `V1`, `V2`, and `V3` in order.
- A non-empty database without Flyway history is baselined at version 0, so Flyway also runs the idempotent `V1` table-creation statements before `V2` and `V3`. Existing tables are not recreated; `V3` adds known later columns and aligns several column lengths. Review a backup of any legacy database before first migration.
- `V3__appointment_integrity.sql` adds known later columns, aligns entity column lengths, and ensures appointment status and uniqueness constraints. It intentionally fails if existing duplicate appointment data prevents the constraints from being created. Resolve those duplicates and restart the application.
- If Flyway reports a checksum mismatch for `V1` or another already-applied migration, do not disable validation or blindly run `flyway repair`. Restore the original applied migration file or perform a deliberate, backed-up migration repair after verifying the database schema and migration history.

The database enforces that non-cancelled appointments cannot occupy the same time slot and that a patient cannot have more than one `SCHEDULED` or `CONFIRMED` appointment on the same date. The service also validates these rules to provide useful error messages; database constraints are the final protection against concurrent requests.

## Security configuration

- `JWT_SECRET`: required; at least 32 bytes for HS256. Startup validation rejects missing/short secrets and non-positive token expiry.
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: database connection configuration.
- `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME`: optional initial doctor account settings.
- `CLINIC_TIMEZONE`: defaults to `Asia/Kolkata`.
- `BOOKING_HORIZON_DAYS`: defaults to 60.
- `CANCELLATION_CUTOFF_HOURS`: defaults to 2. `0` is valid (no cutoff). The API returns `patientCanChange` and `changeCutoffHours` on every appointment, so the UI follows this setting automatically.
- `RATE_LIMIT_LOGIN_MAX`, `RATE_LIMIT_REGISTER_MAX`, `RATE_LIMIT_WINDOW_SECONDS`, `RATE_LIMIT_MAX_ENTRIES`: in-memory login/registration rate limit settings.
- `BEHIND_PROXY` and `TRUSTED_PROXIES`: only enable proxy mode when the app is reachable through explicitly trusted proxy IP addresses. The proxy must append/sanitize `X-Forwarded-For`; wildcard trusted-proxy configuration is rejected. Do not expose the app directly while trusting forwarded headers.

The rate limiter is in-memory and applies per application instance. For multiple backend instances, use a shared rate-limit store such as Redis. For production, also consider moving JWTs from `localStorage` to appropriately configured HttpOnly, Secure, SameSite cookies with CSRF protection.

## Main features

### Patients
- Register and log in
- View availability and book appointments
- View, reschedule, and cancel their own appointments subject to the configured cutoff
- View completed-visit prescriptions and download them as PDFs
- Maintain allergies and medical history

### Doctor/admin
- Dashboard and reports
- Search, filter, sort, and paginate patients and appointments
- Confirm, complete, cancel, and reschedule appointments
- View patient appointment history and prescriptions
- Update patient medical notes with audit records

## Verification

Backend tests:

```bash
cd backend
./mvnw clean test
# Windows: mvnw.cmd clean test
```

Frontend production build:

```bash
cd frontend
npm ci
npm run build
```

Before deployment, test migrations against both a fresh database and a copy of the existing database, and test simultaneous booking requests against PostgreSQL.


## Automated tests

Run the unit and service tests without Docker:

```bash
cd backend
mvnw.cmd test
```

Run the full verification suite, including real PostgreSQL integration tests:

```bash
mvnw.cmd verify
```

The `*IT` tests use Testcontainers and require Docker Desktop to be running. They verify the
Spring Security filter chain and PostgreSQL appointment uniqueness indexes against a real
PostgreSQL instance. If Docker is unavailable, Testcontainers skips those integration tests;
that is not a substitute for running them before release.


### Frontend API URL

The optional `VITE_API_URL` is documented in `frontend/.env.example`. Leave it unset for
local development so Vite proxies `/api` to Spring Boot. Vite variables are public build-time
configuration; never put database credentials or JWT signing secrets in a `VITE_` variable.
