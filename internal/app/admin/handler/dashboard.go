package handler

import (
	"net/http"
	"sort"
	"time"

	db "github.com/remorac/sebaya-app/internal/database/sqlc"
)

// adherenceBand is one bucket of the compliance-score distribution.
type adherenceBand struct {
	Label string
	Tone  string // pill tone: success | warning | danger | muted
	Count int
	Pct   int
}

// armStat compares a study arm (intervensi/kontrol) for the research write-up.
type armStat struct {
	Key   string
	Label string
	Count int
	Avg   int
}

// groupStat is one row of the group adherence leaderboard.
type groupStat struct {
	Name    string
	Members int
	Avg     int
}

// activityItem is one entry of the recent-activity feed, derived from the audit trail.
type activityItem struct {
	Icon  string
	Tone  string
	Label string
	Table string
	When  time.Time
}

// Dashboard renders the admin overview: headline counts plus an adherence
// overview (distribution, study-arm comparison, group leaderboard) and a
// recent-activity feed. Everything is composed from existing store methods.
func (h *Handler) Dashboard(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	day := todayLocal()

	patientCount, _ := h.store.CountPatients(ctx, db.CountPatientsParams{Search: ""})
	groupCount, _ := h.store.CountPeerGroups(ctx)
	activeToday, _ := h.store.CountPatientsActiveOn(ctx, db.CountPatientsActiveOnParams{Day: day})

	rows := h.buildAdherence(r)

	// Average score + distribution bands.
	total := len(rows)
	sum := 0
	high, mid, low, none := 0, 0, 0, 0
	for _, ro := range rows {
		sum += ro.ScorePercent
		switch {
		case ro.ScorePercent == 0:
			none++
		case ro.ScorePercent <= 33:
			low++
		case ro.ScorePercent <= 66:
			mid++
		default:
			high++
		}
	}
	avg := 0
	if total > 0 {
		avg = sum / total
	}
	pctOf := func(n int) int {
		if total == 0 {
			return 0
		}
		return n * 100 / total
	}
	bands := []adherenceBand{
		{Label: "Tinggi · ≥67%", Tone: "success", Count: high, Pct: pctOf(high)},
		{Label: "Sedang · 34–66%", Tone: "warning", Count: mid, Pct: pctOf(mid)},
		{Label: "Rendah · 1–33%", Tone: "danger", Count: low, Pct: pctOf(low)},
		{Label: "Belum ada data", Tone: "muted", Count: none, Pct: pctOf(none)},
	}

	// Study-arm comparison.
	armSum := map[string]int{}
	armN := map[string]int{}
	for _, ro := range rows {
		if !ro.User.StudyArm.Valid {
			continue
		}
		k := string(ro.User.StudyArm.UserStudyArm)
		armSum[k] += ro.ScorePercent
		armN[k]++
	}
	armAvg := func(k string) int {
		if armN[k] == 0 {
			return 0
		}
		return armSum[k] / armN[k]
	}
	arms := []armStat{
		{Key: "intervensi", Label: "Intervensi", Count: armN["intervensi"], Avg: armAvg("intervensi")},
		{Key: "kontrol", Label: "Kontrol", Count: armN["kontrol"], Avg: armAvg("kontrol")},
	}

	// Group leaderboard (top 5 by average score).
	gSum := map[string]int{}
	gN := map[string]int{}
	for _, ro := range rows {
		if ro.GroupName == "" {
			continue
		}
		gSum[ro.GroupName] += ro.ScorePercent
		gN[ro.GroupName]++
	}
	groups := make([]groupStat, 0, len(gN))
	for name, n := range gN {
		groups = append(groups, groupStat{Name: name, Members: n, Avg: gSum[name] / n})
	}
	sort.Slice(groups, func(i, j int) bool {
		if groups[i].Avg != groups[j].Avg {
			return groups[i].Avg > groups[j].Avg
		}
		return groups[i].Name < groups[j].Name
	})
	if len(groups) > 5 {
		groups = groups[:5]
	}

	// Recent activity from the audit trail.
	audits, _ := h.store.ListAuditTrails(ctx, db.ListAuditTrailsParams{Limit: 8, Offset: 0})
	recent := make([]activityItem, 0, len(audits))
	for _, a := range audits {
		recent = append(recent, activityItem{
			Icon:  auditIcon(a.Action),
			Tone:  auditTone(a.Action),
			Label: auditLabel(a.Action, a.TableName),
			Table: a.TableName,
			When:  a.CreatedAt,
		})
	}

	h.render(w, r, "dashboard", map[string]any{
		"ActiveNav":    "dashboard",
		"Title":        "Dashboard",
		"Subtitle":     "Ringkasan kepatuhan dan aktivitas responden.",
		"PatientCount": patientCount,
		"GroupCount":   groupCount,
		"ActiveToday":  activeToday,
		"AvgAdherence": avg,
		"WindowDays":   monitorWindowDays,
		"Bands":        bands,
		"Arms":         arms,
		"Groups":       groups,
		"Recent":       recent,
	})
}

func auditLabel(action, table string) string {
	act := map[string]string{"INSERT": "Tambah", "UPDATE": "Ubah", "DELETE": "Hapus", "UPSERT": "Catat"}[action]
	if act == "" {
		act = action
	}
	tbl := map[string]string{
		"user":              "responden",
		"peer_group":        "kelompok",
		"education_article": "artikel",
		"medication_log":    "log obat",
		"activity_log":      "log aktivitas",
		"diet_log":          "log diet",
		"glucose_log":       "log gula darah",
	}[table]
	if tbl == "" {
		tbl = table
	}
	return act + " " + tbl
}

func auditIcon(action string) string {
	switch action {
	case "INSERT":
		return "plus"
	case "DELETE":
		return "trash-2"
	case "UPSERT":
		return "check"
	default:
		return "pencil"
	}
}

func auditTone(action string) string {
	switch action {
	case "INSERT", "UPSERT":
		return "success"
	case "DELETE":
		return "danger"
	default:
		return "info"
	}
}
