-- Apply once to an existing database before deploying the pet feature.
CREATE TABLE IF NOT EXISTS pet (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    personality_prompt TEXT NOT NULL,
    preview_path VARCHAR(255) DEFAULT NULL,
    atlas_path VARCHAR(255) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=draft,1=published',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_pet_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pet_grant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    granted_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_pet (student_id, pet_id),
    KEY idx_pet_grant_pet (pet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO pet (id, name, description, personality_prompt, preview_path, atlas_path, status)
VALUES (1, '学习伙伴', '陪你一起完成代码挑战的互动宠物',
        '你是一只友善、活泼的少儿编程学习宠物。使用简短、温暖的中文回答，鼓励学生自己思考。遇到编程问题先给提示，不直接提供完整作业答案。',
        'builtin:preview', 'builtin:atlas', 1)
ON DUPLICATE KEY UPDATE id = id;
