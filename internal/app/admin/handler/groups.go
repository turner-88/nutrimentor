package handler

import (
	"net/http"
	"strings"

	"github.com/go-chi/chi/v5"

	db "github.com/remorac/nutrimentor-app/internal/database/sqlc"
	mw "github.com/remorac/nutrimentor-app/internal/shared/middleware"
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

// memberStat is one row of a group's member roster with 30-day adherence.
type memberStat struct {
	User          db.User
	CompliantDays int64
	ScorePercent  int
}

// GroupDetail renders a peer group's profile, edit form, and member roster
// with each member's 30-day adherence (same metric as Monitoring).
func (h *Handler) GroupDetail(w http.ResponseWriter, r *http.Request) {
	id := atoi32(chi.URLParam(r, "id"))
	ctx := r.Context()
	g, err := h.store.GetPeerGroup(ctx, id)
	if err != nil {
		http.NotFound(w, r)
		return
	}
	users, _ := h.store.ListPatientsByGroup(ctx, toNullInt32(id))

	to := todayLocal()
	from := to.AddDate(0, 0, -(monitorWindowDays - 1))
	members := make([]memberStat, 0, len(users))
	sum := 0
	for _, u := range users {
		days, _ := h.store.CountCompliantDays(ctx, db.CountCompliantDaysParams{UserID: u.ID, LogDate: from, LogDate_2: to})
		score := int(days * 100 / monitorWindowDays)
		sum += score
		members = append(members, memberStat{User: u, CompliantDays: days, ScorePercent: score})
	}
	avg := 0
	if len(members) > 0 {
		avg = sum / len(members)
	}

	h.render(w, r, "group_detail", map[string]any{
		"ActiveNav":  "groups",
		"Title":      g.Name,
		"Subtitle":   "Profil kelompok dan kepatuhan anggota.",
		"G":          g,
		"Members":    members,
		"AvgScore":   avg,
		"WindowDays": monitorWindowDays,
	})
}

// NewGroupForm renders the create-group form.
func (h *Handler) NewGroupForm(w http.ResponseWriter, r *http.Request) {
	h.render(w, r, "group_form", map[string]any{
		"ActiveNav": "groups",
		"Title":     "Kelompok Baru",
		"IsNew":     true,
		"G":         db.PeerGroup{},
	})
}

// EditGroupForm renders the edit-group form.
func (h *Handler) EditGroupForm(w http.ResponseWriter, r *http.Request) {
	id := atoi32(chi.URLParam(r, "id"))
	g, err := h.store.GetPeerGroup(r.Context(), id)
	if err != nil {
		http.NotFound(w, r)
		return
	}
	h.render(w, r, "group_form", map[string]any{
		"ActiveNav": "groups",
		"Title":     "Ubah Kelompok",
		"IsNew":     false,
		"G":         g,
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
	http.Redirect(w, r, "/admin/groups/"+chi.URLParam(r, "id"), http.StatusFound)
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
