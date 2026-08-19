package handler

import (
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/remorac/sebaya-app/internal/shared/util"
)

type articleListItem struct {
	ID             int32  `json:"id"`
	Title          string `json:"title"`
	Slug           string `json:"slug"`
	Category       string `json:"category"`
	CoverImagePath string `json:"cover_image_path"`
}

// ListEducation returns published education articles (summaries).
func (h *Handler) ListEducation(w http.ResponseWriter, r *http.Request) {
	rows, err := h.store.ListPublishedArticles(r.Context())
	if err != nil {
		util.WriteInternalError(w, "Gagal memuat artikel.")
		return
	}
	out := make([]articleListItem, 0, len(rows))
	for _, a := range rows {
		out = append(out, articleListItem{
			ID: a.ID, Title: a.Title, Slug: a.Slug,
			Category: a.Category, CoverImagePath: a.CoverImagePath,
		})
	}
	util.WriteSuccess(w, "", out)
}

// GetEducation returns a single published article by slug (with body).
func (h *Handler) GetEducation(w http.ResponseWriter, r *http.Request) {
	slug := chi.URLParam(r, "slug")
	a, err := h.store.GetArticleBySlug(r.Context(), slug)
	if err != nil {
		util.WriteNotFound(w, "Artikel tidak ditemukan.")
		return
	}
	util.WriteSuccess(w, "", map[string]any{
		"id":               a.ID,
		"title":            a.Title,
		"slug":             a.Slug,
		"category":         a.Category,
		"cover_image_path": a.CoverImagePath,
		"body_html":        a.BodyHtml,
	})
}
