-- V3: Align legacy schemas and enforce appointment invariants for fresh and baselined databases.
-- Resolve duplicate data manually before applying this migration; it intentionally fails loudly.

-- Columns introduced after the earliest app versions. Safe no-ops on fresh schemas.
ALTER TABLE patients ADD COLUMN IF NOT EXISTS allergies VARCHAR(1000);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS medical_history VARCHAR(2000);
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS follow_up_date DATE;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE medicines ADD COLUMN IF NOT EXISTS instructions VARCHAR(300);

-- Keep column lengths consistent with current JPA entity mappings.
ALTER TABLE prescriptions ALTER COLUMN diagnosis TYPE VARCHAR(500);
ALTER TABLE medicines ALTER COLUMN name TYPE VARCHAR(200);
ALTER TABLE medicines ALTER COLUMN frequency TYPE VARCHAR(100);
ALTER TABLE medicines ALTER COLUMN instructions TYPE VARCHAR(300);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM appointments
        WHERE status <> 'CANCELLED'
        GROUP BY appointment_time
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot create ux_appointments_active_slot: multiple non-cancelled appointments share a time. Resolve duplicates and rerun Flyway.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM appointments
        WHERE status IN ('SCHEDULED', 'CONFIRMED')
        GROUP BY patient_id, CAST(appointment_time AS date)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot create ux_appointments_patient_active_day: a patient has multiple active appointments on the same date. Resolve duplicates and rerun Flyway.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM patients
        GROUP BY user_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot enforce unique patient user_id: duplicate patient profiles reference the same user. Resolve duplicates and rerun Flyway.';
    END IF;
END $$;

-- Replace legacy status checks that may not allow CONFIRMED.
DO $$
DECLARE constraint_row RECORD;
BEGIN
    FOR constraint_row IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'appointments'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE appointments DROP CONSTRAINT %I', constraint_row.conname);
    END LOOP;
END $$;

ALTER TABLE appointments
    ADD CONSTRAINT appointments_status_check
    CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED'));

CREATE UNIQUE INDEX IF NOT EXISTS ux_appointments_active_slot
    ON appointments (appointment_time)
    WHERE status <> 'CANCELLED';

CREATE UNIQUE INDEX IF NOT EXISTS ux_appointments_patient_active_day
    ON appointments (patient_id, CAST(appointment_time AS date))
    WHERE status IN ('SCHEDULED', 'CONFIRMED');

CREATE UNIQUE INDEX IF NOT EXISTS uk_patients_user_id
    ON patients (user_id);
