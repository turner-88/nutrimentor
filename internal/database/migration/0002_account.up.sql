-- Account self-service: user email + password reset tokens.

ALTER TABLE `user`
    ADD COLUMN email VARCHAR(190) NULL AFTER pekerjaan,
    ADD UNIQUE KEY uq_user_email (email);

CREATE TABLE password_reset_token (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT         NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME    NOT NULL,
    used_at    DATETIME    NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_prt_hash (token_hash),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
