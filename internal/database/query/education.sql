-- name: CreateArticle :execresult
INSERT INTO education_article (title, slug, category, cover_image_path, body_html, is_published, sort_order)
VALUES (?, ?, ?, ?, ?, ?, ?);

-- name: UpdateArticle :exec
UPDATE education_article
SET title = ?, slug = ?, category = ?, cover_image_path = ?, body_html = ?, is_published = ?, sort_order = ?
WHERE id = ?;

-- name: DeleteArticle :exec
DELETE FROM education_article WHERE id = ?;

-- name: GetArticleByID :one
SELECT * FROM education_article WHERE id = ? LIMIT 1;

-- name: GetArticleBySlug :one
SELECT * FROM education_article WHERE slug = ? AND is_published = 1 LIMIT 1;

-- name: ListPublishedArticles :many
SELECT * FROM education_article WHERE is_published = 1 ORDER BY sort_order ASC, id DESC;

-- name: ListAllArticles :many
SELECT * FROM education_article ORDER BY sort_order ASC, id DESC;
