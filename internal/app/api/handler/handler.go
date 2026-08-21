package handler

import (
	"net/http"
	"time"

	"github.com/remorac/nutrimentor-app/internal/database/store"
	"github.com/remorac/nutrimentor-app/internal/shared/config"
	"github.com/remorac/nutrimentor-app/internal/shared/middleware"
	"github.com/remorac/nutrimentor-app/internal/shared/model"
	"github.com/remorac/nutrimentor-app/internal/shared/util"
)

// Handler holds dependencies for the JSON API consumed by the Android app.
type Handler struct {
	config *config.Config
	store  *store.Store
}

func New(cfg *config.Config, s *store.Store) *Handler {
	return &Handler{config: cfg, store: s}
}

// Health is a public liveness probe.
func (h *Handler) Health(w http.ResponseWriter, r *http.Request) {
	util.WriteSuccess(w, "API is healthy", map[string]any{
		"status": "ok",
		"time":   time.Now().Unix(),
	})
}

// currentUser returns the authenticated user from context (never nil on
// protected routes because RequireAuth runs first).
func (h *Handler) currentUser(r *http.Request) *model.User {
	return middleware.GetUserFromContext(r.Context())
}

// today returns the current local date truncated to midnight.
func today() time.Time {
	n := time.Now()
	return time.Date(n.Year(), n.Month(), n.Day(), 0, 0, 0, 0, time.Local)
}

// parseDate parses a YYYY-MM-DD query value, returning def when empty/invalid.
func parseDate(v string, def time.Time) time.Time {
	if v == "" {
		return def
	}
	t, err := time.ParseInLocation("2006-01-02", v, time.Local)
	if err != nil {
		return def
	}
	return t
}
