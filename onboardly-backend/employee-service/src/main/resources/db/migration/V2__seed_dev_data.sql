-- ============================================================
-- V2 — Dev seed data (local only — do not run in production)
-- ============================================================

-- HR employee (auth-service user_id = 1)
INSERT INTO employee (user_id, first_name, last_name, middle_name, preferred_name,
                      email, cell_phone, gender, ssn, date_of_birth,
                      citizenship_status, employment_start_date)
VALUES (1, 'James', 'Wilson', 'Robert', 'Jim',
        'james.wilson@company.com', '555-100-0001', 'MALE', 'ENC_PLACEHOLDER', '1985-03-22',
        'CITIZEN', '2020-01-15');

-- Regular employee / NON_RESIDENT (auth-service user_id = 2)
INSERT INTO employee (user_id, first_name, last_name, middle_name, preferred_name,
                      email, cell_phone, gender, ssn, date_of_birth,
                      citizenship_status, employment_start_date)
VALUES (2, 'Emily', 'Chen', NULL, 'Emily',
        'emily.chen@company.com', '555-100-0002', 'FEMALE', 'ENC_PLACEHOLDER', '1995-07-14',
        'NON_RESIDENT', '2024-06-01');

-- Addresses (resolved by user_id to avoid hard-coding auto-increment IDs)
INSERT INTO address (employee_id, type, address_line1, city, state, zip_code)
VALUES ((SELECT id FROM employee WHERE user_id = 1),
        'PRIMARY', '123 Main Street', 'Seattle', 'WA', '98101');

INSERT INTO address (employee_id, type, address_line1, city, state, zip_code)
VALUES ((SELECT id FROM employee WHERE user_id = 2),
        'PRIMARY', '456 Oak Avenue Apt 3B', 'Seattle', 'WA', '98102');

-- Emergency contacts
INSERT INTO contact (employee_id, type, first_name, last_name, relationship, cell_phone, email)
VALUES ((SELECT id FROM employee WHERE user_id = 1),
        'EMERGENCY', 'Sarah', 'Wilson', 'Spouse', '555-200-0001', 'sarah.wilson@email.com');

INSERT INTO contact (employee_id, type, first_name, last_name, relationship, cell_phone, email)
VALUES ((SELECT id FROM employee WHERE user_id = 2),
        'EMERGENCY', 'Wei', 'Chen', 'Parent', '555-200-0002', 'wei.chen@email.com');

-- Visa status for NON_RESIDENT employee (F1-OPT)
INSERT INTO visa_status (employee_id, visa_type, active_flag, start_date, end_date)
VALUES ((SELECT id FROM employee WHERE user_id = 2),
        'F1_OPT', TRUE, '2024-06-01', '2025-06-01');

-- Onboarding applications
INSERT INTO onboarding_application (employee_id, status, submitted_at, reviewed_at, reviewed_by)
VALUES ((SELECT id FROM employee WHERE user_id = 1),
        'APPROVED', '2020-01-16 09:00:00', '2020-01-17 10:00:00', 1);

INSERT INTO onboarding_application (employee_id, status, submitted_at)
VALUES ((SELECT id FROM employee WHERE user_id = 2),
        'PENDING', '2024-06-03 09:00:00');
