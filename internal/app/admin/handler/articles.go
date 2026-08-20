package handler

import (
	"context"
	"fmt"
	"html/template"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"

	db "github.com/remorac/sebaya-app/internal/database/sqlc"
	mw "github.com/remorac/sebaya-app/internal/shared/middleware"
)

// ListArticles renders all education articles.
func (h *Handler) ListArticles(w http.ResponseWriter, r *http.Request) {
	articles, _ := h.store.ListAllArticles(r.Context())
	h.render(w, r, "articles", map[string]any{
		"ActiveNav": "articles",
		"Title":     "Edukasi",
		"Articles":  articles,
	})
}

// ArticleDetail renders the read-only article detail page.
func (h *Handler) ArticleDetail(w http.ResponseWriter, r *http.Request) {
	id := atoi32(chi.URLParam(r, "id"))
	a, err := h.store.GetArticleByID(r.Context(), id)
	if err != nil {
		http.NotFound(w, r)
		return
	}
	h.render(w, r, "article_detail", map[string]any{
		"ActiveNav": "articles",
		"Title":     a.Title,
		"A":         a,
		"Body":      template.HTML(a.BodyHtml), // admin-authored, trusted HTML
	})
}

// NewArticleForm renders the create-article form.
func (h *Handler) NewArticleForm(w http.ResponseWriter, r *http.Request) {
	h.render(w, r, "article_form", map[string]any{
		"ActiveNav": "articles",
		"Title":     "Artikel Baru",
		"IsNew":     true,
		"A":         db.EducationArticle{IsPublished: true},
	})
}

// EditArticleForm renders the edit-article form.
func (h *Handler) EditArticleForm(w http.ResponseWriter, r *http.Request) {
	id := atoi32(chi.URLParam(r, "id"))
	a, err := h.store.GetArticleByID(r.Context(), id)
	if err != nil {
		http.NotFound(w, r)
		return
	}
	h.render(w, r, "article_form", map[string]any{
		"ActiveNav": "articles",
		"Title":     "Ubah Artikel",
		"IsNew":     false,
		"A":         a,
	})
}

// SaveArticle handles create and update (multipart form with optional cover image).
func (h *Handler) SaveArticle(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseMultipartForm(8 << 20); err != nil {
		http.Error(w, "Form tidak valid.", http.StatusBadRequest)
		return
	}
	actor := mw.GetUserFromContext(r.Context()).ID
	id := atoi32(chi.URLParam(r, "id"))

	title := strings.TrimSpace(r.FormValue("title"))
	slug := slugify(title)

	// Ensure slug uniqueness (the column is UNIQUE and admins can no longer set it manually).
	existing, _ := h.store.ListAllArticles(r.Context())
	taken := make(map[string]bool, len(existing))
	for _, a := range existing {
		if a.ID == id { // exclude self on edit so an unchanged title keeps its slug
			continue
		}
		taken[a.Slug] = true
	}
	slug = uniqueSlug(slug, taken)
	published := r.FormValue("is_published") == "on" || r.FormValue("is_published") == "1"
	sortOrder := atoi32(r.FormValue("sort_order"))

	cover := strings.TrimSpace(r.FormValue("existing_cover"))
	if path, ok := h.saveCover(r); ok {
		cover = path
	}

	// Notify patients only when an article becomes newly published.
	newlyPublished := false

	if id == 0 {
		if _, err := h.store.CreateArticleTx(r.Context(), db.CreateArticleParams{
			Title: title, Slug: slug, Category: strings.TrimSpace(r.FormValue("category")),
			CoverImagePath: cover, BodyHtml: r.FormValue("body_html"),
			IsPublished: published, SortOrder: sortOrder,
		}, actor); err != nil {
			http.Error(w, "Gagal menyimpan: "+err.Error(), http.StatusInternalServerError)
			return
		}
		newlyPublished = published
	} else {
		prev, _ := h.store.GetArticleByID(r.Context(), id)
		if err := h.store.UpdateArticleTx(r.Context(), db.UpdateArticleParams{
			Title: title, Slug: slug, Category: strings.TrimSpace(r.FormValue("category")),
			CoverImagePath: cover, BodyHtml: r.FormValue("body_html"),
			IsPublished: published, SortOrder: sortOrder, ID: id,
		}, actor); err != nil {
			http.Error(w, "Gagal menyimpan: "+err.Error(), http.StatusInternalServerError)
			return
		}
		newlyPublished = published && !prev.IsPublished
	}

	if newlyPublished && h.notify != nil {
		// Fire-and-forget so the admin request is not blocked on FCM.
		go h.notify.NewArticle(context.Background(), title, slug)
	}

	http.Redirect(w, r, "/admin/articles", http.StatusFound)
}

// DeleteArticle removes an article.
func (h *Handler) DeleteArticle(w http.ResponseWriter, r *http.Request) {
	id := atoi32(chi.URLParam(r, "id"))
	actor := mw.GetUserFromContext(r.Context()).ID
	if err := h.store.DeleteArticleTx(r.Context(), id, actor); err != nil {
		http.Error(w, "Gagal.", http.StatusInternalServerError)
		return
	}
	http.Redirect(w, r, "/admin/articles", http.StatusFound)
}

// saveCover stores an uploaded cover image under static/uploads and returns its
// public path. Returns ok=false when no file was uploaded.
func (h *Handler) saveCover(r *http.Request) (string, bool) {
	file, hdr, err := r.FormFile("cover")
	if err != nil {
		return "", false
	}
	defer file.Close()

	ext := strings.ToLower(filepath.Ext(hdr.Filename))
	switch ext {
	case ".jpg", ".jpeg", ".png", ".webp", ".gif":
	default:
		return "", false
	}
	if err := os.MkdirAll("static/uploads", 0o755); err != nil {
		return "", false
	}
	name := fmt.Sprintf("article_%d%s", time.Now().UnixNano(), ext)
	dst, err := os.Create(filepath.Join("static/uploads", name))
	if err != nil {
		return "", false
	}
	defer dst.Close()
	if _, err := io.Copy(dst, file); err != nil {
		return "", false
	}
	return "/static/uploads/" + name, true
}
