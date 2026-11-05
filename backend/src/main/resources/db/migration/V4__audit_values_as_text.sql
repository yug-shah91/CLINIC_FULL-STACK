-- V4: keep complete audit values.
-- The audit trail must store what actually changed, so these columns become unbounded TEXT.
-- Input size is bounded where the data enters the system (request validation), not here.
ALTER TABLE medical_record_audits ALTER COLUMN old_value TYPE TEXT;
ALTER TABLE medical_record_audits ALTER COLUMN new_value TYPE TEXT;
ALTER TABLE medical_record_audits ALTER COLUMN details   TYPE TEXT;
