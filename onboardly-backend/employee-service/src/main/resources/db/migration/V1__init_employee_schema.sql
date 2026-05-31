-- ============================================================
-- V1 — Initial schema for Employee Service
-- ============================================================

CREATE TABLE employee (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id                     BIGINT          NOT NULL,
    first_name                  VARCHAR(50)     NOT NULL,
    last_name                   VARCHAR(50)     NOT NULL,
    middle_name                 VARCHAR(50),
    preferred_name              VARCHAR(50),
    email                       VARCHAR(100)    NOT NULL,
    cell_phone                  VARCHAR(20),
    alternate_phone             VARCHAR(20),
    gender                      ENUM('MALE','FEMALE','OTHER','PREFER_NOT_TO_SAY'),
    ssn                         VARCHAR(255),
    date_of_birth               DATE,
    avatar_url                  VARCHAR(500)    DEFAULT '/avatars/default.png',
    citizenship_status          ENUM('CITIZEN','GREEN_CARD','NON_RESIDENT') NOT NULL,
    driver_license              VARCHAR(50),
    driver_license_expiration   DATE,
    house_id                    BIGINT,
    employment_start_date       DATE,
    employment_end_date         DATE,
    created_at                  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_employee_user_id (user_id),
    UNIQUE KEY uq_employee_email (email)
);

CREATE TABLE address (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    employee_id     BIGINT          NOT NULL,
    type            ENUM('PRIMARY','SECONDARY') NOT NULL,
    address_line1   VARCHAR(200)    NOT NULL,
    address_line2   VARCHAR(200),
    city            VARCHAR(100)    NOT NULL,
    state           VARCHAR(50)     NOT NULL,
    zip_code        VARCHAR(20)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_address_employee
        FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE,
    INDEX idx_address_employee_id (employee_id)
);

-- No UNIQUE on (employee_id, type) — EMERGENCY allows multiple rows per employee.
-- Max-1-REFERENCE is enforced in the service layer.
CREATE TABLE contact (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    employee_id     BIGINT          NOT NULL,
    type            ENUM('REFERENCE','EMERGENCY') NOT NULL,
    first_name      VARCHAR(50)     NOT NULL,
    last_name       VARCHAR(50)     NOT NULL,
    middle_name     VARCHAR(50),
    cell_phone      VARCHAR(20),
    alternate_phone VARCHAR(20),
    email           VARCHAR(100),
    relationship    VARCHAR(50),
    address         VARCHAR(300),
    PRIMARY KEY (id),
    CONSTRAINT fk_contact_employee
        FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE,
    INDEX idx_contact_employee_type (employee_id, type)
);

CREATE TABLE visa_status (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    employee_id             BIGINT          NOT NULL,
    visa_type               VARCHAR(50)     NOT NULL,
    visa_type_other         VARCHAR(100),
    active_flag             BOOLEAN         NOT NULL DEFAULT TRUE,
    start_date              DATE,
    end_date                DATE,
    last_modification_date  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_visa_status_employee
        FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE,
    INDEX idx_visa_status_employee_id (employee_id)
);

CREATE TABLE personal_document (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    employee_id             BIGINT          NOT NULL,
    document_type           VARCHAR(50)     NOT NULL,
    s3_key                  VARCHAR(500)    NOT NULL,
    title                   VARCHAR(200),
    comment                 TEXT,
    application_type        VARCHAR(50),
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modification_date  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_personal_document_employee
        FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE,
    INDEX idx_personal_document_employee_id (employee_id)
);

CREATE TABLE onboarding_application (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    employee_id     BIGINT          NOT NULL,
    status          ENUM('NOT_STARTED','PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'NOT_STARTED',
    hr_feedback     TEXT,
    submitted_at    TIMESTAMP       NULL,
    reviewed_at     TIMESTAMP       NULL,
    reviewed_by     BIGINT          NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_onboarding_employee
        FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE,
    INDEX idx_employee_status (employee_id, status)
);

CREATE TABLE document_feedback (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    personal_document_id    BIGINT          NOT NULL,
    hr_user_id              BIGINT          NOT NULL,
    comment                 TEXT            NOT NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_document_feedback_document
        FOREIGN KEY (personal_document_id) REFERENCES personal_document(id) ON DELETE CASCADE,
    INDEX idx_document_feedback_document_id (personal_document_id)
);
