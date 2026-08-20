-- name: UpsertDeviceToken :execresult
INSERT INTO device_token (user_id, token, platform)
VALUES (?, ?, ?)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), platform = VALUES(platform);

-- name: DeleteDeviceToken :exec
DELETE FROM device_token WHERE token = ?;

-- name: ListActivePatientTokens :many
SELECT dt.token
FROM device_token dt
JOIN `user` u ON u.id = dt.user_id
WHERE u.role = 'patient' AND u.is_active = 1;

-- name: ListPatientTokensWithUnreadArticles :many
-- Patients who have at least one published article they have not yet read.
SELECT DISTINCT dt.token
FROM device_token dt
JOIN `user` u ON u.id = dt.user_id
WHERE u.role = 'patient' AND u.is_active = 1
  AND EXISTS (
      SELECT 1 FROM education_article a
      WHERE a.is_published = 1
        AND NOT EXISTS (
            SELECT 1 FROM article_read ar
            WHERE ar.user_id = u.id AND ar.article_id = a.id
        )
  );

-- name: ListPatientTokensNeedingDailyLog :many
-- Patients missing any of today's daily logs: medication, activity, diet, or glucose.
SELECT DISTINCT dt.token
FROM device_token dt
JOIN `user` u ON u.id = dt.user_id
LEFT JOIN medication_log m ON m.user_id = u.id AND m.log_date = sqlc.arg(day)
LEFT JOIN activity_log a ON a.user_id = u.id AND a.log_date = sqlc.arg(day)
LEFT JOIN diet_log d ON d.user_id = u.id AND d.log_date = sqlc.arg(day)
LEFT JOIN glucose_log g ON g.user_id = u.id AND DATE(g.measured_at) = sqlc.arg(day)
WHERE u.role = 'patient' AND u.is_active = 1
  AND (m.id IS NULL OR a.id IS NULL OR d.id IS NULL OR g.id IS NULL);
