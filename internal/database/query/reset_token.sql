-- name: CreatePasswordResetToken :execresult
INSERT INTO password_reset_token (user_id, token_hash, expires_at)
VALUES (?, ?, ?);

-- name: GetValidResetToken :one
SELECT * FROM password_reset_token
WHERE token_hash = ? AND used_at IS NULL AND expires_at > NOW()
LIMIT 1;

-- name: MarkResetTokenUsed :exec
UPDATE password_reset_token SET used_at = NOW() WHERE id = ?;
