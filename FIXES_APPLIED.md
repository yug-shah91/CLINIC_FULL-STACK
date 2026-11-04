# Fixes applied in this revision

- Replaced the rate limiter's concurrent map/queue implementation with synchronized bounded sliding-window stores. Request recording, capacity enforcement, and scheduled cleanup now share the same lock, preventing cleanup races and capacity overshoot.
- Added a concurrent rate-limiter regression test.
- Changed trusted-proxy IP extraction to walk `X-Forwarded-For` right-to-left and choose the first non-trusted address. Wildcard trusted-proxy configuration is rejected. The proxy must append/sanitize the header as documented.
- Added startup validation for non-positive JWT expiration.
- Added Flyway V3 migration to add known later columns, align current JPA column lengths, replace legacy appointment status checks, and enforce unique appointment slot, one active appointment per patient per day, and unique patient-user mapping. The migration fails clearly when existing duplicates prevent constraints from being installed.
- Set Flyway baseline version to 0 so a non-empty database without Flyway history can run idempotent V1 table creation before later migrations. Existing databases should still be backed up and reviewed before migration.
- Updated README to remove obsolete instructions about the deleted DatabaseConstraintFixer and Hibernate schema updates.

## Verification status

The Maven test suite could not be run in this environment because the Maven wrapper could not download Maven from Maven Central. Run `mvnw.cmd clean test` from `backend` on your machine, then test migrations on a backup of your existing PostgreSQL database and on a fresh empty database. The migration is designed to fail rather than silently ignore duplicate data.
