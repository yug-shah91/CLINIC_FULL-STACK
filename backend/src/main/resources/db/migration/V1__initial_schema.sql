-- V1: Initial schema for Clinic Management Application

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_email_lower_check CHECK (email = LOWER(TRIM(email)))
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    phone VARCHAR(15),
    gender VARCHAR(10),
    address VARCHAR(200),
    blood_group VARCHAR(15),
    allergies VARCHAR(1000),
    medical_history VARCHAR(2000),
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_patients_user_id UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_patients_phone ON patients(phone);

CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    appointment_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(300),
    booked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT appointments_status_check CHECK (status IN ('SCHEDULED','CONFIRMED','COMPLETED','CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_time ON appointments(appointment_time);
CREATE UNIQUE INDEX IF NOT EXISTS ux_appointments_active_slot
    ON appointments (appointment_time) WHERE status <> 'CANCELLED';
CREATE UNIQUE INDEX IF NOT EXISTS ux_appointments_patient_active_day
    ON appointments (patient_id, CAST(appointment_time AS date))
    WHERE status IN ('SCHEDULED', 'CONFIRMED');

CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE REFERENCES appointments(id),
    diagnosis VARCHAR(200) NOT NULL,
    notes VARCHAR(1000),
    follow_up_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS medicines (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    dosage VARCHAR(50) NOT NULL,
    frequency VARCHAR(50) NOT NULL,
    duration_days INT NOT NULL,
    instructions VARCHAR(200)
);

CREATE INDEX IF NOT EXISTS idx_medicines_prescription ON medicines(prescription_id);
