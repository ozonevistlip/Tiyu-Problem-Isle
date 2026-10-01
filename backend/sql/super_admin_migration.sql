USE cppkid_oj;

-- Run this once for databases created with an older schema.sql.
ALTER TABLE `user`
    ADD COLUMN last_login_at DATETIME DEFAULT NULL AFTER status,
    ADD COLUMN last_active_at DATETIME DEFAULT NULL AFTER last_login_at;

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
    status TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    published_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_announcement_status (status, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
