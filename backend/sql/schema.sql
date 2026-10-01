CREATE DATABASE IF NOT EXISTS cppkid_oj DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE cppkid_oj;

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    real_name VARCHAR(64) DEFAULT NULL,
    nickname VARCHAR(64) DEFAULT NULL,
    role VARCHAR(20) NOT NULL COMMENT 'teacher/student/SUPER_ADMIN',
    created_by BIGINT DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    last_login_at DATETIME DEFAULT NULL,
    last_active_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    KEY idx_role (role),
    KEY idx_user_creator (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_session (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ip_address VARCHAR(64) DEFAULT NULL,
    user_agent VARCHAR(500) DEFAULT NULL,
    expires_at DATETIME NOT NULL,
    last_active_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_session_user_status (user_id, status),
    KEY idx_session_online (status, last_active_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS site_setting (
    setting_key VARCHAR(64) PRIMARY KEY,
    setting_value VARCHAR(1000) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO site_setting (setting_key, setting_value)
VALUES ('registration_enabled', 'false')
ON DUPLICATE KEY UPDATE setting_key = VALUES(setting_key);

CREATE TABLE IF NOT EXISTS site_visit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    visitor_key VARCHAR(100) NOT NULL,
    path VARCHAR(255) DEFAULT NULL,
    ip_address VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_visit_created_at (created_at),
    KEY idx_visit_visitor (visitor_key, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=draft,1=published',
    created_by BIGINT NOT NULL,
    published_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_announcement_status (status, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS class_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lesson (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_type_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    lesson_order INT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=draft,1=published',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_type_order (class_type_id, lesson_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lesson_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lesson_id BIGINT NOT NULL,
    tier VARCHAR(20) NOT NULL,
    kind VARCHAR(20) NOT NULL COMMENT 'VIDEO or HOMEWORK',
    title VARCHAR(150) NOT NULL,
    content TEXT DEFAULT NULL,
    file_path VARCHAR(500) DEFAULT NULL,
    file_name VARCHAR(255) DEFAULT NULL,
    mime_type VARCHAR(100) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_lesson_tier (lesson_id, tier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS class_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    class_type_id BIGINT DEFAULT NULL,
    class_name VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_teacher_id (teacher_id),
    KEY idx_class_type_id (class_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS class_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_class_student (class_id, student_id),
    KEY idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lesson_student_tier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lesson_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    tier VARCHAR(20) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_lesson_class_student (lesson_id, class_id, student_id),
    KEY idx_class_lesson (class_id, lesson_id),
    KEY idx_student_lesson (student_id, lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS problem (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    description MEDIUMTEXT NOT NULL,
    input_format MEDIUMTEXT DEFAULT NULL,
    output_format MEDIUMTEXT DEFAULT NULL,
    sample_input MEDIUMTEXT DEFAULT NULL,
    sample_output MEDIUMTEXT DEFAULT NULL,
    difficulty VARCHAR(20) NOT NULL DEFAULT 'easy',
    time_limit_ms INT NOT NULL DEFAULT 2000,
    memory_limit_mb INT NOT NULL DEFAULT 128,
    compare_mode VARCHAR(50) NOT NULL DEFAULT 'ignore_trailing_space',
    created_by BIGINT NOT NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'private',
    status TINYINT NOT NULL DEFAULT 1,
    accepted_count INT NOT NULL DEFAULT 0,
    submit_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_created_by (created_by),
    KEY idx_visibility (visibility)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS testcase (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    problem_id BIGINT NOT NULL,
    input_data MEDIUMTEXT NOT NULL,
    output_data MEDIUMTEXT NOT NULL,
    score INT NOT NULL DEFAULT 100,
    sort_order INT NOT NULL DEFAULT 0,
    is_sample TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_problem_order (problem_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS problem_hint (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    problem_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    hint_title VARCHAR(100) DEFAULT NULL,
    hint_content MEDIUMTEXT NOT NULL,
    hint_level INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_problem_level (problem_id, hint_level),
    KEY idx_teacher_id (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contest (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(1000) DEFAULT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    show_rank TINYINT NOT NULL DEFAULT 1,
    allow_submit_after_end TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_teacher_id (teacher_id),
    KEY idx_class_status (class_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contest_problem (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    contest_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    score INT NOT NULL DEFAULT 100,
    hint_unlock_minutes INT NOT NULL DEFAULT 10,
    show_hint_after_ac TINYINT NOT NULL DEFAULT 1,
    show_hint_after_contest TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_contest_problem (contest_id, problem_id),
    KEY idx_problem_id (problem_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contest_participant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    contest_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    total_score INT NOT NULL DEFAULT 0,
    accepted_count INT NOT NULL DEFAULT 0,
    submit_count INT NOT NULL DEFAULT 0,
    started_at DATETIME DEFAULT NULL,
    last_submit_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_contest_student (contest_id, student_id),
    KEY idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contest_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    contest_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UNTRIED',
    best_score INT NOT NULL DEFAULT 0,
    submit_count INT NOT NULL DEFAULT 0,
    first_open_at DATETIME DEFAULT NULL,
    first_submit_at DATETIME DEFAULT NULL,
    accepted_at DATETIME DEFAULT NULL,
    last_submit_at DATETIME DEFAULT NULL,
    hint_shown TINYINT NOT NULL DEFAULT 0,
    hint_first_shown_at DATETIME DEFAULT NULL,
    final_submission_id BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_contest_problem_student (contest_id, problem_id, student_id),
    KEY idx_contest_student (contest_id, student_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    contest_id BIGINT DEFAULT NULL,
    language VARCHAR(30) NOT NULL,
    code MEDIUMTEXT NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    score INT NOT NULL DEFAULT 0,
    time_used_ms INT NOT NULL DEFAULT 0,
    memory_used_kb INT NOT NULL DEFAULT 0,
    error_message MEDIUMTEXT DEFAULT NULL,
    judged_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_problem_id (problem_id),
    KEY idx_contest_user (contest_id, user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS submission_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    testcase_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    time_used_ms INT NOT NULL DEFAULT 0,
    memory_used_kb INT NOT NULL DEFAULT 0,
    input_preview TEXT DEFAULT NULL,
    expected_output_preview TEXT DEFAULT NULL,
    actual_output_preview TEXT DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_submission_id (submission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
