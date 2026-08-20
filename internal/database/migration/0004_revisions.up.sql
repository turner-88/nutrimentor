-- Revision additions (Tambahan Revisi):
--   * per-user article read progress (article_read)
--   * activity third question: weekly moderate-exercise days
--   * diet question: limiting sugar/salt/fat & more vegetables

CREATE TABLE article_read (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT      NOT NULL,
    article_id INT      NOT NULL,
    read_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_article_read (user_id, article_id),
    KEY idx_article_read_user (user_id),
    CONSTRAINT fk_read_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
    CONSTRAINT fk_read_article FOREIGN KEY (article_id) REFERENCES education_article(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE activity_log
    ADD COLUMN exercise_days_per_week INT NOT NULL DEFAULT 0;

ALTER TABLE diet_log
    ADD COLUMN limit_sugar_salt_fat TINYINT(1) NOT NULL DEFAULT 0;
