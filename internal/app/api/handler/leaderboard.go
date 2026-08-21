package handler

import (
	"net/http"
	"sort"

	db "github.com/remorac/nutrimentor-app/internal/database/sqlc"
	"github.com/remorac/nutrimentor-app/internal/shared/util"
)

// adherenceWindowDays is the rolling window over which the leaderboard scores
// patients by number of fully-compliant days (all three daily pillars met).
const adherenceWindowDays = 30

type leaderboardEntry struct {
	UserID       int32  `json:"user_id"`
	NamaLengkap  string `json:"nama_lengkap"`
	CompliantDays int64 `json:"compliant_days"`
	ScorePercent int    `json:"score_percent"`
	Rank         int    `json:"rank"`
	IsMe         bool   `json:"is_me"`
}

// Leaderboard ranks the members of the caller's peer group by adherence.
func (h *Handler) Leaderboard(w http.ResponseWriter, r *http.Request) {
	me, err := h.store.GetUserByID(r.Context(), h.currentUser(r).ID)
	if err != nil {
		util.WriteInternalError(w, "Terjadi kesalahan pada server.")
		return
	}
	if !me.GroupID.Valid {
		util.WriteSuccess(w, "Anda belum tergabung dalam kelompok sebaya.", []leaderboardEntry{})
		return
	}

	members, err := h.store.ListPatientsByGroup(r.Context(), me.GroupID)
	if err != nil {
		util.WriteInternalError(w, "Gagal memuat kelompok.")
		return
	}

	to := today()
	from := to.AddDate(0, 0, -(adherenceWindowDays - 1))

	entries := make([]leaderboardEntry, 0, len(members))
	for _, m := range members {
		days, _ := h.store.CountCompliantDays(r.Context(), db.CountCompliantDaysParams{
			UserID: m.ID, LogDate: from, LogDate_2: to,
		})
		entries = append(entries, leaderboardEntry{
			UserID:        m.ID,
			NamaLengkap:   m.NamaLengkap,
			CompliantDays: days,
			ScorePercent:  int(days * 100 / adherenceWindowDays),
			IsMe:          m.ID == me.ID,
		})
	}

	sort.SliceStable(entries, func(i, j int) bool {
		return entries[i].CompliantDays > entries[j].CompliantDays
	})
	for i := range entries {
		entries[i].Rank = i + 1
	}

	util.WriteSuccess(w, "", entries)
}
