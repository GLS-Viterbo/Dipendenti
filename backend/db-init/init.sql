--- HR MODULE ---
CREATE TABLE IF NOT EXISTS companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS employees (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    name VARCHAR(50) NOT NULL,
    surname VARCHAR(50) NOT NULL,
    tax_code VARCHAR(20) UNIQUE,
    birthday DATE,
    address VARCHAR(40),
    city VARCHAR(40),
    email VARCHAR(60),
    phone VARCHAR(15),
    note VARCHAR(200),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS employee_documents (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT REFERENCES employees(id),
    file_name VARCHAR(200) NOT NULL,
    file_path TEXT NOT NULL,
    mime_type VARCHAR(100),
    description TEXT,
    uploaded_at TIMESTAMPTZ DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE employee_deadlines (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT REFERENCES employees(id),
    type VARCHAR(50) NOT NULL,
    expiration_date DATE NOT NULL,
    note VARCHAR(200),
    reminder_days INT DEFAULT 30,
    recipient_email VARCHAR(100),
    notified BOOLEAN DEFAULT FALSE
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) UNIQUE NOT NULL
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    email VARCHAR(100),
    company_id BIGINT REFERENCES companies(id),
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE user_roles (
    user_id BIGINT REFERENCES users(id),
    role_id BIGINT REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS contracts (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    start_date DATE NOT NULL,
    end_date DATE,
    monthly_working_hours INT,
    valid BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS employee_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS group_members (
    group_id BIGINT NOT NULL REFERENCES employee_groups(id),
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    PRIMARY KEY (group_id, employee_id)
);

--- ACCESS MODULE ---

CREATE TABLE IF NOT EXISTS cards (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    uid VARCHAR(100) UNIQUE NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS card_assignments (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    card_id BIGINT NOT NULL REFERENCES cards(id),
    start_date DATE NOT NULL,
    end_date DATE
);

CREATE TABLE IF NOT EXISTS access_logs (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    card_id BIGINT NOT NULL REFERENCES cards(id),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    type VARCHAR(10) NOT NULL,
    modified BOOLEAN NOT NULL DEFAULT FALSE,
    modified_at TIMESTAMP WITH TIME ZONE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_access_logs_timestamp ON access_logs(timestamp);

CREATE TABLE holiday (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    recurring BOOLEAN NOT NULL DEFAULT FALSE,
    day SMALLINT NOT NULL,
    month SMALLINT NOT NULL,
    year SMALLINT NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE absence (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    hours_count INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE employee_leave_accrual (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL UNIQUE REFERENCES employees(id),
    vacation_hours_per_month DECIMAL(5,2) NOT NULL DEFAULT 0,
    rol_hours_per_month DECIMAL(5,2) NOT NULL DEFAULT 0
);

CREATE TABLE employee_leave_balance (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL UNIQUE REFERENCES employees(id),
    vacation_available DECIMAL(5,2) NOT NULL DEFAULT 0,
    rol_available DECIMAL(5,2) NOT NULL DEFAULT 0
);

CREATE TABLE shifts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE shift_associations (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    shift_id BIGINT NOT NULL REFERENCES shifts(id),
    day_of_week SMALLINT NOT NULL
);

CREATE TABLE shift_assignments(
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    auto_generated BOOLEAN NOT NULL DEFAULT FALSE,
    modified_at TIMESTAMPTZ,
    note VARCHAR(100)
);

CREATE TABLE job_tracker(
    id BIGSERIAL PRIMARY KEY,
    job_name VARCHAR(100) UNIQUE NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    last_successful_run_date TIMESTAMPTZ,
    next_scheduled_run_date TIMESTAMPTZ NOT NULL,
    enabled BOOLEAN DEFAULT TRUE
);

INSERT INTO job_tracker (job_name, job_type, next_scheduled_run_date)
VALUES
    ('monthly_accrual', 'MONTHLY', DATE_TRUNC('month', CURRENT_DATE + INTERVAL '1 month')),
    ('shift_generation', 'DAILY', CURRENT_DATE + INTERVAL '1 day'),
    ('deadline_notification', 'DAILY', CURRENT_DATE + INTERVAL '1 day' + TIME '09:00:00')
ON CONFLICT (job_name) DO NOTHING;

-- Crea un ruolo ADMIN
INSERT INTO roles (name) VALUES ('ADMIN');

-- Crea un utente admin (password: admin123)
INSERT INTO users (username, password_hash, email, active)
VALUES ('admin', '$2a$10$kal3iynvRWkLwsIpyXMRf.lPREnqAIL1qq5yMEIkAtHl4n.fpyfDK', 'alexandru.imbrea@gls-italt.com', true);

-- Assegna ruolo ADMIN all'utente
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);


