#!/usr/bin/env bash
# Rehearses the Flyway migrations against a throwaway Postgres database, including a database
# that already holds audit records. Needs psql and a role allowed to create databases.
#
#   PGHOST=localhost PGUSER=postgres PGPASSWORD=... ./scripts/verify-migrations.sh
#
# It creates clinic_migration_check, runs V1-V3 by hand (as an already-deployed database would
# have), inserts audit rows (one very long), then applies V4 and V5 and checks the result.
set -euo pipefail
DB=clinic_migration_check
DIR="$(cd "$(dirname "$0")/../backend/src/main/resources/db/migration" && pwd)"
psql -v ON_ERROR_STOP=1 -d postgres -qc "DROP DATABASE IF EXISTS $DB" -c "CREATE DATABASE $DB"
run() { psql -v ON_ERROR_STOP=1 -d "$DB" -q "$@"; }

for f in V1__initial_schema.sql V2__medical_record_audits.sql V3__appointment_integrity.sql; do
  run -f "$DIR/$f"
done

run -c "INSERT INTO medical_record_audits (entity_name, entity_id, action, performed_by, old_value, new_value, details, performed_at)
        VALUES ('Patient', 1, 'UPDATE', 'doctor@clinic.com', 'before', 'after', 'short', now()),
               ('Prescription', 2, 'CREATE', 'doctor@clinic.com', NULL, repeat('x', 3999), repeat('d', 999), now());"
BEFORE=$(run -tA -c "SELECT count(*), sum(length(new_value)) FROM medical_record_audits")

run -f "$DIR/V4__audit_values_as_text.sql"
run -f "$DIR/V5__remove_duplicate_patient_user_unique.sql"
AFTER=$(run -tA -c "SELECT count(*), sum(length(new_value)) FROM medical_record_audits")
[ "$BEFORE" = "$AFTER" ] || { echo "FAIL: audit data changed: $BEFORE -> $AFTER"; exit 1; }

# A value longer than the old 4000 limit must now be accepted
run -c "INSERT INTO medical_record_audits (entity_name, entity_id, action, performed_by, new_value, performed_at)
        VALUES ('Prescription', 3, 'CREATE', 'doctor@clinic.com', repeat('y', 20000), now());"

TYPES=$(run -tA -c "SELECT string_agg(data_type, ',') FROM information_schema.columns
                    WHERE table_name='medical_record_audits' AND column_name IN ('old_value','new_value','details')")
[ "$TYPES" = "text,text,text" ] || { echo "FAIL: audit column types are $TYPES"; exit 1; }

UNIQUES=$(run -tA -c "SELECT count(*) FROM pg_index i WHERE i.indrelid='public.patients'::regclass
                      AND i.indisunique AND NOT i.indisprimary AND i.indnatts=1
                      AND (SELECT attname FROM pg_attribute WHERE attrelid=i.indrelid AND attnum=i.indkey[0])='user_id'")
[ "$UNIQUES" = "1" ] || { echo "FAIL: $UNIQUES unique indexes on patients.user_id (expected 1)"; exit 1; }

# Uniqueness must still be enforced
run -c "INSERT INTO users (name,email,password,role) VALUES ('A','a@x.com','h','PATIENT'),('B','b@x.com','h','PATIENT')" >/dev/null
run -c "INSERT INTO patients (name,user_id) SELECT 'A', id FROM users WHERE email='a@x.com'" >/dev/null
if run -c "INSERT INTO patients (name,user_id) SELECT 'A2', id FROM users WHERE email='a@x.com'" 2>/dev/null; then
  echo "FAIL: duplicate patient profile for one user was accepted"; exit 1
fi
echo "OK: V4 and V5 applied cleanly, audit rows preserved, user_id still unique."
psql -d postgres -qc "DROP DATABASE $DB"
