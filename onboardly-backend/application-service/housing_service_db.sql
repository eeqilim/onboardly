CREATE DATABASE IF NOT EXISTS housing_service_db;
USE housing_service_db;

DROP TABLE IF EXISTS facility_report_detail;
DROP TABLE IF EXISTS facility_report;
DROP TABLE IF EXISTS facility;
DROP TABLE IF EXISTS house;
DROP TABLE IF EXISTS landlord;

CREATE TABLE landlord (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    cell_phone VARCHAR(255) NOT NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modification_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE house (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    landlord_id BIGINT NOT NULL,
    address VARCHAR(255) NOT NULL,
    max_occupant INT NOT NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modification_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_house_landlord
        FOREIGN KEY (landlord_id) REFERENCES landlord(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE facility (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    house_id BIGINT NOT NULL,
    type VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    quantity INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_facility_house
        FOREIGN KEY (house_id) REFERENCES house(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE facility_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    facility_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modification_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_facility_report_facility
        FOREIGN KEY (facility_id) REFERENCES facility(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE facility_report_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    facility_report_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    comment VARCHAR(2000) NOT NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modification_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_facility_report_detail_report
        FOREIGN KEY (facility_report_id) REFERENCES facility_report(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE INDEX idx_facility_house_id ON facility(house_id);
CREATE INDEX idx_facility_report_facility_id ON facility_report(facility_id);
CREATE INDEX idx_facility_report_employee_id ON facility_report(employee_id);
CREATE INDEX idx_facility_report_status ON facility_report(status);
CREATE INDEX idx_facility_report_detail_report_id ON facility_report_detail(facility_report_id);
CREATE INDEX idx_facility_report_detail_employee_id ON facility_report_detail(employee_id);