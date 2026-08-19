DROP TABLE IF EXISTS password_reset_token;

ALTER TABLE `user`
    DROP KEY uq_user_email,
    DROP COLUMN email;
