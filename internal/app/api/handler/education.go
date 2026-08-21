package handler

import (
	"html"
	"image"
	_ "image/gif"
	_ "image/jpeg"
	_ "image/png"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"strings"

	"github.com/go-chi/chi/v5"
	"github.com/remorac/nutrimentor-app/internal/shared/util"
)

type articleListItem struct {
	ID             int32  `json:"id"`
	Title          string `json:"title"`
	Slug           string `json:"slug"`
	Category       string `json:"category"`
	CoverImagePath string `json:"cover_image_path"`
	Excerpt        string `json:"excerpt"`
}

var htmlTagRE = regexp.MustCompile(`<[^>]*>`)

// excerptFromHTML derives a plain-text summary from article body HTML,
// truncated at a word boundary up to maxLen characters.
func excerptFromHTML(s string, maxLen int) string {
	text := html.UnescapeString(htmlTagRE.ReplaceAllString(s, " "))
	text = strings.TrimSpace(strings.Join(strings.Fields(text), " "))
	if len(text) <= maxLen {
		return text
	}
	cut := text[:maxLen]
	if i := strings.LastIndex(cut, " "); i > 0 {
		cut = cut[:i]
	}
	return strings.TrimSpace(cut) + "…"
}

// coverDimensions returns the pixel width/height of an uploaded cover image, or
// 0,0 when it can't be read (missing file, unknown format). coverPath is the
// public path like "/static/uploads/x.jpeg"; files live on disk under the same
// relative path from the server's working directory.
func coverDimensions(coverPath string) (int, int) {
	rel := strings.TrimPrefix(coverPath, "/")
	if !strings.HasPrefix(rel, "static/uploads/") {
		return 0, 0
	}
	f, err := os.Open(filepath.Clean(rel))
	if err != nil {
		return 0, 0
	}
	defer f.Close()
	cfg, _, err := image.DecodeConfig(f)
	if err != nil {
		return 0, 0
	}
	return cfg.Width, cfg.Height
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
			Excerpt: excerptFromHTML(a.BodyHtml, 140),
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
	coverW, coverH := coverDimensions(a.CoverImagePath)
	util.WriteSuccess(w, "", map[string]any{
		"id":               a.ID,
		"title":            a.Title,
		"slug":             a.Slug,
		"category":         a.Category,
		"cover_image_path": a.CoverImagePath,
		"cover_width":      coverW,
		"cover_height":     coverH,
		"body_html":        a.BodyHtml,
	})
}

// MarkEducationRead records that the current user has read the given article.
func (h *Handler) MarkEducationRead(w http.ResponseWriter, r *http.Request) {
	slug := chi.URLParam(r, "slug")
	a, err := h.store.GetArticleBySlug(r.Context(), slug)
	if err != nil {
		util.WriteNotFound(w, "Artikel tidak ditemukan.")
		return
	}
	if err := h.store.MarkArticleReadTx(r.Context(), h.currentUser(r).ID, a.ID); err != nil {
		util.WriteInternalError(w, "Gagal menyimpan progres membaca.")
		return
	}
	util.WriteSuccess(w, "", nil)
}
