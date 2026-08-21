package handler

import (
	"encoding/csv"
	"net/http"
	"strconv"
	"time"

	db "github.com/remorac/nutrimentor-app/internal/database/sqlc"
)

// monitorWindowDays is the rolling adherence window used by monitoring/exports.
const monitorWindowDays = 30

type adherenceRow struct {
	User          db.User
	GroupName     string
	CompliantDays int64
	ScorePercent  int
}

// buildAdherence computes the adherence rows for all patients over the window.
func (h *Handler) buildAdherence(r *http.Request) []adherenceRow {
	ctx := r.Context()
	to := todayLocal()
	from := to.AddDate(0, 0, -(monitorWindowDays - 1))

	groups, _ := h.store.ListPeerGroups(ctx)
	groupName := map[int32]string{}
	for _, g := range groups {
		groupName[g.ID] = g.Name
	}

	patients, _ := h.store.ListPatients(ctx, db.ListPatientsParams{Search: "", Limit: 1000, Offset: 0})
	rows := make([]adherenceRow, 0, len(patients))
	for _, p := range patients {
		days, _ := h.store.CountCompliantDays(ctx, db.CountCompliantDaysParams{UserID: p.ID, LogDate: from, LogDate_2: to})
		gn := ""
		if p.GroupID.Valid {
			gn = groupName[p.GroupID.Int32]
		}
		rows = append(rows, adherenceRow{
			User: p, GroupName: gn, CompliantDays: days,
			ScorePercent: int(days * 100 / monitorWindowDays),
		})
	}
	return rows
}

// Monitoring renders the adherence overview table.
func (h *Handler) Monitoring(w http.ResponseWriter, r *http.Request) {
	h.render(w, r, "monitoring", map[string]any{
		"ActiveNav":  "monitoring",
		"Title":      "Monitoring",
		"Rows":       h.buildAdherence(r),
		"WindowDays": monitorWindowDays,
	})
}

// ExportCSV streams the adherence data as a CSV for the research write-up.
func (h *Handler) ExportCSV(w http.ResponseWriter, r *http.Request) {
	rows := h.buildAdherence(r)
	w.Header().Set("Content-Type", "text/csv; charset=utf-8")
	w.Header().Set("Content-Disposition", "attachment; filename=\"nutrimentor_adherence_"+time.Now().Format("20060102")+".csv\"")

	cw := csv.NewWriter(w)
	defer cw.Flush()
	_ = cw.Write([]string{"id", "nama_lengkap", "username", "usia", "jenis_kelamin", "kelompok", "study_arm", "compliant_days", "window_days", "score_percent"})
	for _, r := range rows {
		arm := ""
		if r.User.StudyArm.Valid {
			arm = string(r.User.StudyArm.UserStudyArm)
		}
		_ = cw.Write([]string{
			strconv.Itoa(int(r.User.ID)),
			r.User.NamaLengkap,
			r.User.Username,
			strconv.Itoa(int(r.User.Usia)),
			string(r.User.JenisKelamin),
			r.GroupName,
			arm,
			strconv.FormatInt(r.CompliantDays, 10),
			strconv.Itoa(monitorWindowDays),
			strconv.Itoa(r.ScorePercent),
		})
	}
}
