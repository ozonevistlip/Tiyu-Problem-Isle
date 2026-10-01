USE cppkid_oj;

ALTER TABLE `user` ADD COLUMN created_by BIGINT DEFAULT NULL AFTER role;
ALTER TABLE class_group ADD COLUMN class_type_id BIGINT DEFAULT NULL AFTER teacher_id;
ALTER TABLE class_member ADD COLUMN learning_tier VARCHAR(20) NOT NULL DEFAULT 'BASIC' AFTER student_id;
CREATE INDEX idx_user_creator ON `user` (created_by);
CREATE INDEX idx_class_type_id ON class_group (class_type_id);

-- Legacy students linked to exactly one teacher can be assigned safely.
-- Students linked to multiple teachers remain unowned for manual review.
UPDATE `user` u
JOIN (
    SELECT cm.student_id, MIN(c.teacher_id) AS teacher_id
    FROM class_member cm
    JOIN class_group c ON c.id = cm.class_id
    GROUP BY cm.student_id
    HAVING COUNT(DISTINCT c.teacher_id) = 1
) owner ON owner.student_id = u.id
SET u.created_by = owner.teacher_id
WHERE u.role = 'student' AND u.created_by IS NULL;

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
    status TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_type_order (class_type_id, lesson_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lesson_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lesson_id BIGINT NOT NULL,
    tier VARCHAR(20) NOT NULL,
    kind VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    content TEXT DEFAULT NULL,
    file_path VARCHAR(500) DEFAULT NULL,
    file_name VARCHAR(255) DEFAULT NULL,
    mime_type VARCHAR(100) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_lesson_tier (lesson_id, tier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO site_setting (setting_key, setting_value) VALUES ('registration_enabled', 'false')
ON DUPLICATE KEY UPDATE setting_value = 'false';
