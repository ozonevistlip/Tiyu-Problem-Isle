USE cppkid_oj;

-- Run once after teaching_migration.sql on databases already using class-wide tiers.
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

-- Keep existing choices for lessons that already exist. Future lessons start unset.
INSERT INTO lesson_student_tier (lesson_id, class_id, student_id, tier)
SELECT l.id, c.id, cm.student_id, cm.learning_tier
FROM class_member cm
JOIN class_group c ON c.id = cm.class_id
JOIN lesson l ON l.class_type_id = c.class_type_id
ON DUPLICATE KEY UPDATE tier = VALUES(tier);

ALTER TABLE class_member DROP COLUMN learning_tier;
