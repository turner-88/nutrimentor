package handler

import (
	"database/sql"
	"errors"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"

	db "github.com/remorac/nutrimentor-app/internal/database/sqlc"
	mw "github.com/remorac/nutrimentor-app/internal/shared/middleware"
	"github.com/remorac/nutrimentor-app/internal/shared/util"
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
		"ActiveNav":  "patients",
		"Title":      "Responden",
		"Patients":   patients,
		"Groups":     groups,
		"Search":     search,
		"Page":       page,
		"TotalPages": int((total + int64(limit) - 1) / int64(limit)),
		"Total":      total,
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
	from := to.AddDate(0, 0, -29)
	med, _ := h.store.ListMedicationLogs(ctx, db.ListMedicationLogsParams{UserID: id, FromLogDate: from, ToLogDate: to})
	act, _ := h.store.ListActivityLogs(ctx, db.ListActivityLogsParams{UserID: id, FromLogDate: from, ToLogDate: to})
	diet, _ := h.store.ListDietLogs(ctx, db.ListDietLogsParams{UserID: id, FromLogDate: from, ToLogDate: to})
	glu, _ := h.store.ListGlucoseLogs(ctx, db.ListGlucoseLogsParams{UserID: id, FromMeasuredAt: from, ToMeasuredAt: to.Add(24 * time.Hour)})
	compliant, _ := h.store.CountCompliantDays(ctx, db.CountCompliantDaysParams{UserID: id, LogDate: from, LogDate_2: to})
	groups, _ := h.store.ListPeerGroups(ctx)

	heatmap, summary := buildHeatmap(med, act, diet, from, to)

	h.render(w, r, "patient_detail", map[string]any{
		"ActiveNav":     "patients",
		"Title":         u.NamaLengkap,
		"Subtitle":      "Profil dan riwayat kepatuhan responden.",
		"P":             u,
		"Groups":        groups,
		"Medication":    med,
		"Activity":      act,
		"Diet":          diet,
		"Glucose":       glu,
		"CompliantDays": compliant,
		"WindowDays":    30,
		"Heatmap":       heatmap,
		"Pillars":       summary,
	})
}

// dayCell is one day of the 30-day adherence heatmap.
type dayCell struct {
	Date      time.Time
	HasData   bool
	MedOK     bool
	ActOK     bool
	DietOK    bool
	Compliant bool
	Level     int // 0 none/no-data · 1–3 pillars met
}

// pillarSummary counts how many days each core pillar was fulfilled in the window.
type pillarSummary struct {
	Days       int
	Medication int
	Activity   int
	Diet       int
}

// buildHeatmap indexes the med/act/diet logs by day and produces one cell per
// day in [from,to] (oldest→newest), matching the CountCompliantDays definition
// (medication complete · activity done · diet on-schedule).
func buildHeatmap(med []db.MedicationLog, act []db.ActivityLog, diet []db.DietLog, from, to time.Time) ([]dayCell, pillarSummary) {
	key := func(t time.Time) string { return t.Format("2006-01-02") }
	medOK := map[string]bool{}
	actOK := map[string]bool{}
	dietOK := map[string]bool{}
	seen := map[string]bool{}
	for _, m := range med {
		k := key(m.LogDate)
		medOK[k] = m.TakenComplete
		seen[k] = true
	}
	for _, a := range act {
		k := key(a.LogDate)
		actOK[k] = a.DidActivity
		seen[k] = true
	}
	for _, d := range diet {
		k := key(d.LogDate)
		dietOK[k] = d.OnSchedule
		seen[k] = true
	}

	var cells []dayCell
	var sum pillarSummary
	for day := from; !day.After(to); day = day.AddDate(0, 0, 1) {
		k := key(day)
		c := dayCell{Date: day, MedOK: medOK[k], ActOK: actOK[k], DietOK: dietOK[k], HasData: seen[k]}
		if c.MedOK {
			c.Level++
			sum.Medication++
		}
		if c.ActOK {
			c.Level++
			sum.Activity++
		}
		if c.DietOK {
			c.Level++
			sum.Diet++
		}
		c.Compliant = c.MedOK && c.ActOK && c.DietOK
		cells = append(cells, c)
		sum.Days++
	}
	return cells, sum
}

// UpdatePatient edits all of a patient's data: profile fields plus peer group
// and study arm.
func (h *Handler) UpdatePatient(w http.ResponseWriter, r *http.Request) {
	id := atoi32(chi.URLParam(r, "id"))
	ctx := r.Context()
	_ = r.ParseForm()
	actor := mw.GetUserFromContext(ctx).ID

	nama := strings.TrimSpace(r.FormValue("nama_lengkap"))
	if nama == "" {
		http.Error(w, "Nama lengkap wajib diisi.", http.StatusUnprocessableEntity)
		return
	}
	jk := db.UserJenisKelaminL
	if strings.EqualFold(r.FormValue("jenis_kelamin"), "P") {
		jk = db.UserJenisKelaminP
	}

	var email sql.NullString
	if e := strings.TrimSpace(r.FormValue("email")); e != "" {
		email = sql.NullString{String: e, Valid: true}
		if existing, err := h.store.GetUserByEmail(ctx, email); err == nil && existing.ID != id {
			http.Error(w, "Email sudah digunakan.", http.StatusConflict)
			return
		} else if err != nil && !errors.Is(err, sql.ErrNoRows) {
			http.Error(w, "Gagal.", http.StatusInternalServerError)
			return
		}
	}

	if err := h.store.UpdateUserProfileTx(ctx, db.UpdateUserProfileParams{
		NamaLengkap:  nama,
		Usia:         atoi32(r.FormValue("usia")),
		JenisKelamin: jk,
		Pendidikan:   strings.TrimSpace(r.FormValue("pendidikan")),
		Pekerjaan:    strings.TrimSpace(r.FormValue("pekerjaan")),
		Email:        email,
		ID:           id,
	}, actor); err != nil {
		http.Error(w, "Gagal menyimpan.", http.StatusInternalServerError)
		return
	}

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
	if err := h.store.AssignUserGroupTx(ctx, db.AssignUserGroupParams{
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
