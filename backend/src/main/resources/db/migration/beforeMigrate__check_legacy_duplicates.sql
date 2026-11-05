-- Flyway callback (not a versioned migration, so it has no checksum and can change freely).
-- It runs before every migrate. On a database created by the old Hibernate auto-update it
-- checks for rows that would make V1's unique indexes fail, and stops with a readable message.
-- On a fresh database, or one that is already migrated, it does nothing.
DO $$
BEGIN
    IF to_regclass('public.appointments') IS NOT NULL
       AND to_regclass('public.flyway_schema_history') IS NULL THEN
        IF EXISTS (
            SELECT 1 FROM appointments WHERE status <> 'CANCELLED'
            GROUP BY appointment_time HAVING COUNT(*) > 1
        ) THEN
            RAISE EXCEPTION 'Cannot create ux_appointments_active_slot: several non-cancelled appointments share one time. Resolve the duplicates, then start the app again.';
        END IF;
        IF EXISTS (
            SELECT 1 FROM appointments WHERE status IN ('SCHEDULED', 'CONFIRMED')
            GROUP BY patient_id, CAST(appointment_time AS date) HAVING COUNT(*) > 1
        ) THEN
            RAISE EXCEPTION 'Cannot create ux_appointments_patient_active_day: a patient has several active appointments on one date. Resolve the duplicates, then start the app again.';
        END IF;
    END IF;
END $$;
