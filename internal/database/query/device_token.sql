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

-- name: ListPatientTokensNeedingReminder :many
SELECT dt.token
FROM device_token dt
JOIN `user` u ON u.id = dt.user_id
LEFT JOIN medication_log m ON m.user_id = u.id AND m.log_date = sqlc.arg(day)
WHERE u.role = 'patient' AND u.is_active = 1 AND m.id IS NULL;
