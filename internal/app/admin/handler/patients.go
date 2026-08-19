package handler

import (
	"database/sql"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"

	db "github.com/remorac/sebaya-app/internal/database/sqlc"
	mw "github.com/remorac/sebaya-app/internal/shared/middleware"
	"github.com/remorac/sebaya-app/internal/shared/util"
)

// ListPatients renders the paginated, searchable patient list.
func (h *Handler) ListPatients(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	search := strings.TrimSpace(r.URL.Query().Get("q"))
	page, _ := strconv.Atoi(r.URL.Query().Get("page"))
	if page < 1 {
		page = 1
	}
	limit := h.config.PageSize
	offset := (page - 1) * limit

	patients, _ := h.store.ListPatients(ctx, db.ListPatientsParams{
		Search: search, Limit: int32(limit), Offset: int32(offset),
	})
	total, _ := h.store.CountPatients(ctx, db.CountPatientsParams{Search: search})
	groups, _ := h.store.ListPeerGroups(ctx)

	h.render(w, r, "patients", map[string]any{
		"ActiveNav": "patients",
		"Title":     "Responden",
		"Patients":  patients,
		"Groups":    groups,
		"Search":    search,
		"Page":      page,
		"TotalPages": int((total + int64(limit) - 1) / int64(limit)),
		"Total":     total,
	})
}

// PatientDetail renders a patient's profile and recent logs.
func (h *Handler) PatientDetail(w http.ResponseWriter, r *http.Request) {
	id := atoi32(chi.URLParam(r, "id"))
	ctx := r.Context()
	u, err := h.store.GetUserByID(ctx, id)
	if err != nil {
		http.NotFound(w, r)
		return
	}
	to := todayLocal()
	from := to.AddDate(0, 0, -30)
	med, _ := h.store.ListMedicationLogs(ctx, db.ListMedicationLogsParams{UserID: id, FromLogDate: from, ToLogDate: to})
	act, _ := h.store.ListActivityLogs(ctx, db.ListActivityLogsParams{UserID: id, FromLogDate: from, ToLogDate: to})
	diet, _ := h.store.ListDietLogs(ctx, db.ListDietLogsParams{UserID: id, FromLogDate: from, ToLogDate: to})
	glu, _ := h.store.ListGlucoseLogs(ctx, db.ListGlucoseLogsParams{UserID: id, FromMeasuredAt: from, ToMeasuredAt: to.Add(24 * time.Hour)})
	compliant, _ := h.store.CountCompliantDays(ctx, db.CountCompliantDaysParams{UserID: id, LogDate: from, LogDate_2: to})
	groups, _ := h.store.ListPeerGroups(ctx)

	h.render(w, r, "patient_detail", map[string]any{
		"ActiveNav":     "patients",
		"Title":         u.NamaLengkap,
		"P":             u,
		"Groups":        groups,
		"Medication":    med,
		"Activity":      act,
		"Diet":          diet,
		"Glucose":       glu,
		"CompliantDays": compliant,
	})
}

// AssignGroup sets a patient's peer group and study arm.
func (h *Handler) AssignGroup(w http.ResponseWriter, r *http.Request) {
	id := atoi32(chi.URLParam(r, "id"))
	_ = r.ParseForm()
	actor := mw.GetUserFromContext(r.Context()).ID

	var groupID sql.NullInt32
	if g := atoi32(r.FormValue("group_id")); g > 0 {
		groupID = sql.NullInt32{Int32: g, Valid: true}
	}
	var arm db.NullUserStudyArm
	switch r.FormValue("study_arm") {
	case "intervensi":
		arm = db.NullUserStudyArm{UserStudyArm: db.UserStudyArmIntervensi, Valid: true}
	case "kontrol":
		arm = db.NullUserStudyArm{UserStudyArm: db.UserStudyArmKontrol, Valid: true}
	}

	if err := h.store.AssignUserGroupTx(r.Context(), db.AssignUserGroupParams{
		GroupID: groupID, StudyArm: arm, ID: id,
	}, actor); err != nil {
		http.Error(w, "Gagal menyimpan.", http.StatusInternalServerError)
		return
	}
	http.Redirect(w, r, "/admin/patients/"+chi.URLParam(r, "id"), http.StatusFound)
}

// ResetPatientPassword sets a new password for the patient.
func (h *Handler) ResetPatientPassword(w http.ResponseWriter, r *http.Request) {
	id := atoi32(chi.URLParam(r, "id"))
	_ = r.ParseForm()
	pw := r.FormValue("password")
	if err := util.ValidatePassword(pw); err != nil {
		http.Error(w, err.Error(), http.StatusUnprocessableEntity)
		return
	}
	hash, err := util.HashPassword(pw)
	if err != nil {
		http.Error(w, "Gagal.", http.StatusInternalServerError)
		return
	}
	actor := mw.GetUserFromContext(r.Context()).ID
	if err := h.store.UpdateUserPasswordTx(r.Context(), db.UpdateUserPasswordParams{PasswordHash: hash, ID: id}, actor); err != nil {
		http.Error(w, "Gagal.", http.StatusInternalServerError)
		return
	}
	http.Redirect(w, r, "/admin/patients/"+chi.URLParam(r, "id"), http.StatusFound)
}

// ToggleActive activates/deactivates a patient account.
func (h *Handler) ToggleActive(w http.ResponseWriter, r *http.Request) {
	id := atoi32(chi.URLParam(r, "id"))
	_ = r.ParseForm()
	active := r.FormValue("active") == "1"
	actor := mw.GetUserFromContext(r.Context()).ID
	if err := h.store.SetUserActiveTx(r.Context(), db.SetUserActiveParams{IsActive: active, ID: id}, actor); err != nil {
		http.Error(w, "Gagal.", http.StatusInternalServerError)
		return
	}
	http.Redirect(w, r, "/admin/patients/"+chi.URLParam(r, "id"), http.StatusFound)
}

func atoi32(s string) int32 {
	n, _ := strconv.Atoi(strings.TrimSpace(s))
	return int32(n)
}

func todayLocal() time.Time {
	n := time.Now()
	return time.Date(n.Year(), n.Month(), n.Day(), 0, 0, 0, 0, time.Local)
}
