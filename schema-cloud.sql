-- =====================================================================
--  UE CompLab Monitoring System — database
--  CLOUD VERSION (Aiven / any hosted MySQL 8). Run ONCE from MySQL Workbench,
--  connected as your cloud admin user (Aiven: avnadmin, SSL required).
--  Same tables and sample data as schema.sql, without CREATE USER / GRANT:
--  the apps log in with the cloud admin user instead of labuser.
-- =====================================================================

DROP DATABASE IF EXISTS comlab;
CREATE DATABASE comlab CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE comlab;

-- ---------------------------------------------------------------------
--  Who can log in
-- ---------------------------------------------------------------------
CREATE TABLE students (
    student_no     VARCHAR(20)  PRIMARY KEY,
    full_name      VARCHAR(100) NOT NULL,
    course         VARCHAR(50),
    password_hash  CHAR(64)     NOT NULL,          -- SHA-256 hex
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE admins (
    username       VARCHAR(30)  PRIMARY KEY,
    full_name      VARCHAR(100) NOT NULL,
    password_hash  CHAR(64)     NOT NULL
);

-- ---------------------------------------------------------------------
--  Lab workstations. last_seen is updated every 10 s by the student app
--  ("heartbeat"); the admin dashboard treats a PC as offline after 30 s.
-- ---------------------------------------------------------------------
CREATE TABLE pcs (
    pc_id          VARCHAR(10)  PRIMARY KEY,
    location       VARCHAR(60)  NOT NULL,
    last_seen      DATETIME     NULL
);

-- ---------------------------------------------------------------------
--  One row per lab session (log in -> log out)
-- ---------------------------------------------------------------------
CREATE TABLE sessions (
    session_id     INT AUTO_INCREMENT PRIMARY KEY,
    student_no     VARCHAR(20)  NOT NULL,
    pc_id          VARCHAR(10)  NOT NULL,
    time_in        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    time_out       DATETIME     NULL,
    end_reason     VARCHAR(20)  NULL,               -- LOGOUT / ADMIN / RECOVERED
    FOREIGN KEY (student_no) REFERENCES students(student_no),
    FOREIGN KEY (pc_id)      REFERENCES pcs(pc_id),
    INDEX idx_open (pc_id, time_out)
);

-- ---------------------------------------------------------------------
--  The "Workstation Check" screen (Monitor / Keyboard / Mouse)
-- ---------------------------------------------------------------------
CREATE TABLE equipment_checks (
    check_id       INT AUTO_INCREMENT PRIMARY KEY,
    session_id     INT          NOT NULL UNIQUE,
    monitor_ok     BOOLEAN      NOT NULL,
    keyboard_ok    BOOLEAN      NOT NULL,
    mouse_ok       BOOLEAN      NOT NULL,
    checked_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES sessions(session_id)
);

-- ---------------------------------------------------------------------
--  Problem reports. source = PRECHECK (an unticked item) or STUDENT
--  ("Other concerns" form / "Report issue" during the session)
-- ---------------------------------------------------------------------
CREATE TABLE reports (
    report_id      INT AUTO_INCREMENT PRIMARY KEY,
    session_id     INT          NULL,
    pc_id          VARCHAR(10)  NOT NULL,
    student_no     VARCHAR(20)  NULL,
    category       VARCHAR(30)  NOT NULL,           -- Hardware / Peripheral / Software / Network / Other
    priority       VARCHAR(10)  NOT NULL DEFAULT 'Medium',
    description    TEXT,
    photo          MEDIUMBLOB   NULL,
    photo_name     VARCHAR(120) NULL,
    source         VARCHAR(10)  NOT NULL DEFAULT 'STUDENT',
    status         VARCHAR(12)  NOT NULL DEFAULT 'PENDING',   -- PENDING / IN_PROGRESS / RESOLVED
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    resolved_at    DATETIME     NULL,
    FOREIGN KEY (session_id) REFERENCES sessions(session_id),
    FOREIGN KEY (pc_id)      REFERENCES pcs(pc_id),
    FOREIGN KEY (student_no) REFERENCES students(student_no),
    INDEX idx_status (status)
);

-- ---------------------------------------------------------------------
--  Sample data for the demo
--  Student passwords: 1234     Admin: admin / admin123
-- ---------------------------------------------------------------------
INSERT INTO admins VALUES ('admin', 'Lab Administrator', SHA2('admin123', 256));

INSERT INTO students (student_no, full_name, course, password_hash) VALUES
 ('20241120810', 'Jay Lord Gaspi',   'BSCpE', SHA2('1234', 256)),
 ('20231234567', 'Maria Santos',     'BSCpE', SHA2('1234', 256)),
 ('20231112223', 'Juan Dela Cruz',   'BSECE', SHA2('1234', 256)),
 ('20221098765', 'Angela Reyes',     'BSCS',  SHA2('1234', 256)),
 ('20241001001', 'Mark Villanueva',  'BSIT',  SHA2('1234', 256));

INSERT INTO pcs (pc_id, location) VALUES
 ('PC-01','Lab 6 · CoE Building'),('PC-02','Lab 6 · CoE Building'),('PC-03','Lab 6 · CoE Building'),
 ('PC-04','Lab 6 · CoE Building'),('PC-05','Lab 6 · CoE Building'),('PC-06','Lab 6 · CoE Building'),
 ('PC-07','Lab 6 · CoE Building'),('PC-08','Lab 6 · CoE Building'),('PC-09','Lab 6 · CoE Building'),
 ('PC-10','Lab 6 · CoE Building'),('PC-11','Lab 6 · CoE Building'),('PC-12','Lab 6 · CoE Building'),
 ('PC-13','Lab 6 · CoE Building'),('PC-14','Lab 6 · CoE Building'),('PC-15','Lab 6 · CoE Building'),
 ('PC-16','Lab 6 · CoE Building');

-- a little history so the screens aren't empty
INSERT INTO sessions (student_no, pc_id, time_in, time_out, end_reason) VALUES
 ('20231234567','PC-14', NOW() - INTERVAL 1 DAY - INTERVAL 3 HOUR, NOW() - INTERVAL 1 DAY - INTERVAL 1 HOUR, 'LOGOUT'),
 ('20231112223','PC-03', NOW() - INTERVAL 1 DAY - INTERVAL 5 HOUR, NOW() - INTERVAL 1 DAY - INTERVAL 4 HOUR, 'LOGOUT');

INSERT INTO reports (session_id, pc_id, student_no, category, priority, description, source, status, created_at) VALUES
 (1, 'PC-14', '20231234567', 'Hardware',   'Medium', 'Monitor flickers on startup', 'STUDENT', 'IN_PROGRESS', NOW() - INTERVAL 1 DAY),
 (2, 'PC-03', '20231112223', 'Peripheral', 'Low',    'Mouse scroll wheel sticky',   'STUDENT', 'RESOLVED',    NOW() - INTERVAL 3 DAY);
