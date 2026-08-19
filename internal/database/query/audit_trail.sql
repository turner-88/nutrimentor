-- name: CreateAuditTrail :execresult
INSERT INTO audit_trail (table_name, row_id, action, changes, actor_id)
VALUES (?, ?, ?, ?, ?);

-- name: ListAuditTrails :many
SELECT * FROM audit_trail ORDER BY id DESC LIMIT ? OFFSET ?;

-- name: GetAuditTrail :one
SELECT * FROM audit_trail WHERE id = ? LIMIT 1;
