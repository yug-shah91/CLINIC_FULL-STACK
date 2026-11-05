-- V5: patients.user_id must be unique exactly once.
-- A database created from V1 has two identical unique indexes on it (the inline UNIQUE and
-- uk_patients_user_id), and a legacy database can also carry Hibernate's own. Keep
-- uk_patients_user_id (or, failing that, one of them) and drop the redundant ones.
-- V1 is deliberately not edited: an applied migration must never change.
DO $$
DECLARE
    r    RECORD;
    kept TEXT;
BEGIN
    IF to_regclass('public.patients') IS NULL THEN
        RETURN;
    END IF;

    FOR r IN
        SELECT ic.relname AS index_name, c.conname AS constraint_name
        FROM pg_index i
        JOIN pg_class ic ON ic.oid = i.indexrelid
        LEFT JOIN pg_constraint c ON c.conindid = i.indexrelid AND c.contype = 'u'
        WHERE i.indrelid = 'public.patients'::regclass
          AND i.indisunique AND NOT i.indisprimary
          AND i.indnatts = 1 AND i.indpred IS NULL AND i.indexprs IS NULL
          AND (SELECT a.attname FROM pg_attribute a
               WHERE a.attrelid = i.indrelid AND a.attnum = i.indkey[0]) = 'user_id'
        ORDER BY (ic.relname = 'uk_patients_user_id') DESC, ic.relname
    LOOP
        IF kept IS NULL THEN
            kept := r.index_name;
        ELSIF r.constraint_name IS NOT NULL THEN
            EXECUTE format('ALTER TABLE patients DROP CONSTRAINT %I', r.constraint_name);
        ELSE
            EXECUTE format('DROP INDEX %I', r.index_name);
        END IF;
    END LOOP;
END $$;
