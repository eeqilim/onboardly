CREATE DATABASE IF NOT EXISTS app_db;
USE app_db;

DROP TABLE IF EXISTS digital_document;
DROP TABLE IF EXISTS application_workflow;

CREATE TABLE application_workflow (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modification_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	-- Open, Rejected, Completed, and any NECESSARY STATUS 
    status VARCHAR(50) NOT NULL DEFAULT 'Open',
    -- ONBOARDING_SUBMITTED, I983_DOWNLOADED, I983_UPLOADED, I20_UPLOADED, OPT_RECEIPT_UPLOADED, OPT_EAD_UPLOADED, HR_REVIEW
    current_step VARCHAR(50),
    comment TEXT,
    -- ONBOARDING, OPT_STEM
    application_type VARCHAR(50) NOT NULL
);

CREATE TABLE digital_document (
    id INT AUTO_INCREMENT PRIMARY KEY,
    application_id INT,
    type VARCHAR(50) NOT NULL,
    is_required INT NOT NULL DEFAULT 1,
    path VARCHAR(255) NOT NULL,
    source_document_id VARCHAR(255),
    description TEXT,
    title VARCHAR(255) NOT NULL,
	FOREIGN KEY (application_id) REFERENCES application_workflow(id) ON DELETE CASCADE
);

INSERT INTO application_workflow
(employee_id, create_date, last_modification_date, status, comment, application_type)
VALUES
(2,'2026-05-10 09:00:00','2026-05-10 09:00:00','Open',NULL,'ONBOARDING'),
(2,'2026-05-11 10:00:00','2026-05-13 14:20:00','Completed','Application approved by HR','ONBOARDING'),
(3,'2026-05-15 08:30:00','2026-05-16 11:00:00','Rejected','Missing work authorization document','ONBOARDING'),
(4,'2026-05-20 13:15:00','2026-05-20 13:15:00','Open',NULL,'OPT_STEM');

INSERT INTO digital_document
(application_id,type,path, description,title)
VALUES
(1,'DRIVER_LICENSE','s3://onboardly/alice/driver_license.pdf','New Jersey Driver License','Driver License'),
(1,'WORK_AUTH','s3://onboardly/alice/ead_card.pdf','OPT EAD Card','Employment Authorization'),
(2,'DRIVER_LICENSE','s3://onboardly/bob/driver_license.pdf','Driver License','Driver License'),
(2,'WORK_AUTH','s3://onboardly/bob/h1b.pdf','H1B Approval Notice','H1B Document'),
(3,'WORK_AUTH','s3://onboardly/charlie/incomplete_ead.pdf','Expired work authorization document','Expired EAD'),
(4,'I_983','s3://onboardly/charlie/i983.pdf','STEM OPT Training Plan','I-983'),
(4,'I_20','s3://onboardly/charlie/i20.pdf','Updated I-20 for STEM OPT','I-20');

