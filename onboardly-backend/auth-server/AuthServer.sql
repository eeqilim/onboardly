-- ==========================================
CREATE DATABASE IF NOT EXISTS auth_db;
USE auth_db;

DROP TABLE IF EXISTS registration_tokens;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;

-- users table
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255),
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modification_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 1 is true, 0 is false.
    active_flag INT DEFAULT 1
);

 -- roles Table
 CREATE TABLE roles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(255),
    role_description VARCHAR(255),
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modification_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- user_roles Table
CREATE TABLE user_roles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    active_flag INT DEFAULT 1,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modification_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_role UNIQUE (user_id, role_id)
);

--  registration_tokens table
CREATE TABLE registration_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    expiration_date DATETIME,
    used_flag INT DEFAULT 0,
    create_by INT,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modification_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (create_by) REFERENCES users(id) ON DELETE RESTRICT
);

INSERT INTO users (username, email, password, create_date, last_modification_date, active_flag)
VALUES
('hradmin', 'hradmin@gmail.com', 'admin123', '2026-05-01 09:00:00', '2026-05-01 09:00:00', 1),
('alice', 'alice@gmail.com', '123456', '2026-05-01 09:20:00', '2026-05-01 09:20:00', 1),
('bob', 'bob@gmail.com', '123456', '2026-05-01 09:30:00', '2026-05-01 09:30:00', 1),
('charlie', 'charlie@gmail.com', '123456', '2026-05-01 09:40:00', '2026-05-01 09:40:00', 1);

INSERT INTO roles (role_name, role_description, create_date, last_modification_date)
VALUES('ROLE_HR', 'Human Resources', '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
('ROLE_EMPLOYEE', 'Regular Employee', '2026-01-01 10:00:00', '2026-01-01 10:00:00');

INSERT INTO user_roles (user_id, role_id, active_flag, create_date, last_modification_date)
VALUES
(1, 2, 1, '2026-05-02 09:00:00', '2026-05-02 09:00:00'),
(1, 1, 1, '2026-05-02 09:00:00', '2026-05-02 09:00:00'),
(2, 2, 1, '2026-05-02 09:10:00', '2026-05-02 09:10:00'),
(3, 2, 1, '2026-05-02 09:20:00', '2026-05-02 09:20:00'),
(4, 2, 1, '2026-05-02 09:30:00', '2026-05-02 09:30:00');

INSERT INTO registration_tokens (token, email, expiration_date, used_flag, create_by, create_date, last_modification_date)
VALUES
('hradmintoken', 'hradmin@gmail.com', '2026-06-02 09:00:00', 1, 1, '2026-05-01 09:00:00', '2026-05-01 09:00:00'),
('alicetoken', 'alice@gmail.com', '2026-06-02 09:20:00', 1, 1, '2026-05-01 09:20:00', '2026-05-01 09:20:00'),
('bobtoken', 'bob@gmail.com', '2026-06-02 09:30:00', 1, 1, '2026-05-01 09:30:00', '2026-05-01 09:30:00'),
('charlietoken', 'charlie@gmail.com', '2026-06-02 09:40:00', 1, 1, '2026-05-01 09:40:00', '2026-05-01 09:40:00');

-- ==========================================

