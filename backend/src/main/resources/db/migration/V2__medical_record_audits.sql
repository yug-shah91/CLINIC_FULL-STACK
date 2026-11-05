-- V2: Audit trail for medical records and prescriptions

CREATE TABLE IF NOT EXISTS medical_record_audits (
    id BIGSERIAL PRIMARY KEY,
    entity_name VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    old_value VARCHAR(4000),
    new_value VARCHAR(4000),
    details VARCHAR(1000),
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_entity ON medical_record_audits(entity_name, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_performed_by ON medical_record_audits(performed_by);
