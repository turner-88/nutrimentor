package handler

import (
	"net/http"
	"strconv"

	db "github.com/remorac/nutrimentor-app/internal/database/sqlc"
)

// ListAudit renders the most recent audit-trail entries.
func (h *Handler) ListAudit(w http.ResponseWriter, r *http.Request) {
	page, _ := strconv.Atoi(r.URL.Query().Get("page"))
	if page < 1 {
		page = 1
	}
	limit := 50
	offset := (page - 1) * limit
	rows, _ := h.store.ListAuditTrails(r.Context(), db.ListAuditTrailsParams{
		Limit: int32(limit), Offset: int32(offset),
	})
	h.render(w, r, "audit", map[string]any{
		"ActiveNav": "audit",
		"Title":     "Audit Trail",
		"Rows":      rows,
		"Page":      page,
	})
}
