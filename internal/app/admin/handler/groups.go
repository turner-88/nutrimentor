package handler

import (
	"net/http"
	"strings"

	"github.com/go-chi/chi/v5"

	db "github.com/remorac/sebaya-app/internal/database/sqlc"
	mw "github.com/remorac/sebaya-app/internal/shared/middleware"
)

// ListGroups renders peer groups with member counts.
func (h *Handler) ListGroups(w http.ResponseWriter, r *http.Request) {
	groups, _ := h.store.ListPeerGroups(r.Context())
	type row struct {
		G       db.PeerGroup
		Members int
	}
	rows := make([]row, 0, len(groups))
	for _, g := range groups {
		members, _ := h.store.ListPatientsByGroup(r.Context(), toNullInt32(g.ID))
		rows = append(rows, row{G: g, Members: len(members)})
	}
	h.render(w, r, "groups", map[string]any{
		"ActiveNav": "groups",
		"Title":     "Kelompok Sebaya",
		"Rows":      rows,
	})
}

// CreateGroup adds a new peer group.
func (h *Handler) CreateGroup(w http.ResponseWriter, r *http.Request) {
	_ = r.ParseForm()
	name := strings.TrimSpace(r.FormValue("name"))
	if name == "" {
		http.Error(w, "Nama kelompok wajib diisi.", http.StatusUnprocessableEntity)
		return
	}
	actor := mw.GetUserFromContext(r.Context()).ID
	if _, err := h.store.CreatePeerGroupTx(r.Context(), db.CreatePeerGroupParams{
		Name: name, Description: strings.TrimSpace(r.FormValue("description")),
	}, actor); err != nil {
		http.Error(w, "Gagal.", http.StatusInternalServerError)
		return
	}
	http.Redirect(w, r, "/admin/groups", http.StatusFound)
}

// UpdateGroup edits a peer group.
func (h *Handler) UpdateGroup(w http.ResponseWriter, r *http.Request) {
	id := atoi32(chi.URLParam(r, "id"))
	_ = r.ParseForm()
	actor := mw.GetUserFromContext(r.Context()).ID
	if err := h.store.UpdatePeerGroupTx(r.Context(), db.UpdatePeerGroupParams{
		Name:        strings.TrimSpace(r.FormValue("name")),
		Description: strings.TrimSpace(r.FormValue("description")),
		ID:          id,
	}, actor); err != nil {
		http.Error(w, "Gagal.", http.StatusInternalServerError)
		return
	}
	http.Redirect(w, r, "/admin/groups", http.StatusFound)
}

// DeleteGroup removes a peer group (patients are detached via ON DELETE SET NULL).
func (h *Handler) DeleteGroup(w http.ResponseWriter, r *http.Request) {
	id := atoi32(chi.URLParam(r, "id"))
	actor := mw.GetUserFromContext(r.Context()).ID
	if err := h.store.DeletePeerGroupTx(r.Context(), id, actor); err != nil {
		http.Error(w, "Gagal.", http.StatusInternalServerError)
		return
	}
	http.Redirect(w, r, "/admin/groups", http.StatusFound)
}
