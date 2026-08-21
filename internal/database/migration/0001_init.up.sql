-- NutriMentor initial schema (MariaDB, utf8mb4).

CREATE TABLE peer_group (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120)  NOT NULL,
    description VARCHAR(500)  NOT NULL DEFAULT '',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user` (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    role          ENUM('admin','patient')       NOT NULL DEFAULT 'patient',
    nama_lengkap  VARCHAR(150)                   NOT NULL,
    usia          INT                            NOT NULL DEFAULT 0,
    jenis_kelamin ENUM('L','P')                  NOT NULL DEFAULT 'L',
    pendidikan    VARCHAR(60)                    NOT NULL DEFAULT '',
    pekerjaan     VARCHAR(100)                   NOT NULL DEFAULT '',
    username      VARCHAR(60)                    NOT NULL,
    password_hash VARCHAR(255)                   NOT NULL,
    group_id      INT                            NULL,
    study_arm     ENUM('intervensi','kontrol')   NULL,
    is_active     TINYINT(1)                     NOT NULL DEFAULT 1,
    created_at    DATETIME                       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME                       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_username (username),
    KEY idx_user_group (group_id),
    CONSTRAINT fk_user_group FOREIGN KEY (group_id) REFERENCES peer_group(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE medication_log (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT        NOT NULL,
    log_date       DATE       NOT NULL,
    taken_complete TINYINT(1) NOT NULL DEFAULT 0,
    taken_on_time  TINYINT(1) NOT NULL DEFAULT 0,
    created_at     DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_med (user_id, log_date),
    CONSTRAINT fk_med_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE activity_log (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    user_id          INT        NOT NULL,
    log_date         DATE       NOT NULL,
    did_activity     TINYINT(1) NOT NULL DEFAULT 0,
    per_doctor_advice TINYINT(1) NOT NULL DEFAULT 0,
    created_at       DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_act (user_id, log_date),
    CONSTRAINT fk_act_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE diet_log (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT        NOT NULL,
    log_date          DATE       NOT NULL,
    per_doctor_advice TINYINT(1) NOT NULL DEFAULT 0,
    on_schedule       TINYINT(1) NOT NULL DEFAULT 0,
    created_at        DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_diet (user_id, log_date),
    CONSTRAINT fk_diet_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE glucose_log (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT      NOT NULL,
    measured_at DATETIME NOT NULL,
    timing     ENUM('before_meal','after_meal') NOT NULL,
    value_mgdl INT      NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_glucose_user_time (user_id, measured_at),
    CONSTRAINT fk_glucose_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE education_article (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    slug            VARCHAR(220) NOT NULL,
    category        VARCHAR(80)  NOT NULL DEFAULT '',
    cover_image_path VARCHAR(300) NOT NULL DEFAULT '',
    body_html       LONGTEXT     NOT NULL,
    is_published    TINYINT(1)   NOT NULL DEFAULT 0,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_article_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_trail (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(64) NOT NULL,
    row_id     INT         NOT NULL,
    action     VARCHAR(16) NOT NULL,
    changes    TEXT        NULL,
    actor_id   INT         NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audit_table (table_name, row_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
