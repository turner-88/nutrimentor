-- name: CreatePeerGroup :execresult
INSERT INTO peer_group (name, description) VALUES (?, ?);

-- name: UpdatePeerGroup :exec
UPDATE peer_group SET name = ?, description = ? WHERE id = ?;

-- name: DeletePeerGroup :exec
DELETE FROM peer_group WHERE id = ?;

-- name: GetPeerGroup :one
SELECT * FROM peer_group WHERE id = ? LIMIT 1;

-- name: ListPeerGroups :many
SELECT * FROM peer_group ORDER BY name ASC;

-- name: CountPeerGroups :one
SELECT COUNT(*) FROM peer_group;
