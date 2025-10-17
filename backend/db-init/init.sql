--- HR MODULE ---
CREATE TABLE IF NOT EXISTS employees (
    id BIGSERIAL PRIMARY KEY,
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
