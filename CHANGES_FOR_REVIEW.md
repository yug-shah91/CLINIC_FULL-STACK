# Changes applied for portfolio-quality verification

## Automated testing
- Kept the existing JUnit 5 / Mockito service and exception-handler tests.
- Added Testcontainers PostgreSQL + Maven Failsafe integration-test support.
- Added `ClinicPostgresIT` to verify the real Spring Security 401 JSON response, Flyway-created
  appointment indexes, database rejection of duplicate active bookings, and a simultaneous
  two-request race for the same appointment slot.

## Concurrency
- The new integration test races two independent JDBC inserts against PostgreSQL and asserts
  that exactly one insert succeeds. The database unique indexes remain the final protection.

## Error handling
- Existing `@RestControllerAdvice` already maps validation, conflicts, authentication,
  authorization and unexpected failures to structured JSON. Existing handler tests remain.

## Environment configuration
- Existing root and frontend `.env.example` files are retained.
- Existing `application.properties` reads DB credentials and JWT signing secret from environment.
- Added/confirmed ignore rules for real `.env` files while keeping `.env.example` tracked.
- `VITE_API_URL` remains optional; Vite's `/api` proxy is the local default.

## Verification
- Run `mvnw.cmd test` for unit/service tests.
- Run `mvnw.cmd verify` with Docker running for PostgreSQL/Testcontainers integration tests.
- Run `npm ci && npm run build` in `frontend`.
- Tests were not executed in this editing environment because Maven Wrapper could not download
  Maven from Maven Central. Do not treat the new tests as passing until they run locally.
