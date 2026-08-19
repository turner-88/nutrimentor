-- name: GetUserByUsername :one
SELECT * FROM `user` WHERE username = ? LIMIT 1;

-- name: GetUserByID :one
SELECT * FROM `user` WHERE id = ? LIMIT 1;

-- name: GetUserByEmail :one
SELECT * FROM `user` WHERE email = ? LIMIT 1;

-- name: CreateUser :execresult
INSERT INTO `user` (role, nama_lengkap, usia, jenis_kelamin, pendidikan, pekerjaan, username, password_hash, group_id, study_arm, email)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- name: UpdateUserProfile :exec
UPDATE `user`
SET nama_lengkap = ?, usia = ?, jenis_kelamin = ?, pendidikan = ?, pekerjaan = ?, email = ?
WHERE id = ?;

-- name: UpdateUserPassword :exec
UPDATE `user` SET password_hash = ? WHERE id = ?;

-- name: AssignUserGroup :exec
UPDATE `user` SET group_id = ?, study_arm = ? WHERE id = ?;

-- name: SetUserActive :exec
UPDATE `user` SET is_active = ? WHERE id = ?;

-- name: DeleteUser :exec
DELETE FROM `user` WHERE id = ?;

-- name: ListPatients :many
SELECT * FROM `user`
WHERE role = 'patient'
  AND (sqlc.arg(search) = '' OR nama_lengkap LIKE CONCAT('%', sqlc.arg(search), '%') OR username LIKE CONCAT('%', sqlc.arg(search), '%'))
ORDER BY id DESC
LIMIT ? OFFSET ?;

-- name: CountPatients :one
SELECT COUNT(*) FROM `user`
WHERE role = 'patient'
  AND (sqlc.arg(search) = '' OR nama_lengkap LIKE CONCAT('%', sqlc.arg(search), '%') OR username LIKE CONCAT('%', sqlc.arg(search), '%'));

-- name: ListPatientsByGroup :many
SELECT * FROM `user` WHERE role = 'patient' AND group_id = ? ORDER BY nama_lengkap ASC;

-- name: CountPatientsActiveOn :one
SELECT COUNT(DISTINCT u.id) FROM `user` u
LEFT JOIN medication_log m ON m.user_id = u.id AND m.log_date = sqlc.arg(day)
LEFT JOIN activity_log a ON a.user_id = u.id AND a.log_date = sqlc.arg(day)
LEFT JOIN diet_log d ON d.user_id = u.id AND d.log_date = sqlc.arg(day)
WHERE u.role = 'patient' AND (m.id IS NOT NULL OR a.id IS NOT NULL OR d.id IS NOT NULL);
