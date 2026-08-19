package handler

import (
	"net/http"
	"time"

	db "github.com/remorac/sebaya-app/internal/database/sqlc"
	"github.com/remorac/sebaya-app/internal/shared/util"
)

// Dashboard reports today's completion status of each daily pillar.
func (h *Handler) Dashboard(w http.ResponseWriter, r *http.Request) {
	uid := h.currentUser(r).ID
	day := today()
	ctx := r.Context()

	med, medOK := h.store.GetMedicationLog(ctx, db.GetMedicationLogParams{UserID: uid, LogDate: day})
	act, actOK := h.store.GetActivityLog(ctx, db.GetActivityLogParams{UserID: uid, LogDate: day})
	diet, dietOK := h.store.GetDietLog(ctx, db.GetDietLogParams{UserID: uid, LogDate: day})

	util.WriteSuccess(w, "", map[string]any{
		"date": day.Format("2006-01-02"),
		"medication": map[string]any{
			"logged":         medOK == nil,
			"taken_complete": med.TakenComplete,
			"taken_on_time":  med.TakenOnTime,
		},
		"activity": map[string]any{
			"logged":            actOK == nil,
			"did_activity":      act.DidActivity,
			"per_doctor_advice": act.PerDoctorAdvice,
		},
		"diet": map[string]any{
			"logged":            dietOK == nil,
			"per_doctor_advice": diet.PerDoctorAdvice,
			"on_schedule":       diet.OnSchedule,
		},
	})
}

// --- Medication ---

type medicationRequest struct {
	TakenComplete bool `json:"taken_complete"`
	TakenOnTime   bool `json:"taken_on_time"`
}

func (h *Handler) LogMedication(w http.ResponseWriter, r *http.Request) {
	var req medicationRequest
	if err := util.DecodeJSON(r, &req); err != nil {
		util.WriteBadRequest(w, "Format permintaan tidak valid.")
		return
	}
	err := h.store.UpsertMedicationLogTx(r.Context(), db.UpsertMedicationLogParams{
		UserID:        h.currentUser(r).ID,
		LogDate:       today(),
		TakenComplete: req.TakenComplete,
		TakenOnTime:   req.TakenOnTime,
	})
	if err != nil {
		util.WriteInternalError(w, "Gagal menyimpan catatan.")
		return
	}
	util.WriteSuccess(w, "Catatan konsumsi obat tersimpan.", nil)
}

func (h *Handler) ListMedication(w http.ResponseWriter, r *http.Request) {
	from, to := h.rangeParams(r)
	rows, err := h.store.ListMedicationLogs(r.Context(), db.ListMedicationLogsParams{
		UserID: h.currentUser(r).ID, FromLogDate: from, ToLogDate: to,
	})
	if err != nil {
		util.WriteInternalError(w, "Gagal memuat data.")
		return
	}
	util.WriteSuccess(w, "", rows)
}

// --- Activity ---

type activityRequest struct {
	DidActivity     bool `json:"did_activity"`
	PerDoctorAdvice bool `json:"per_doctor_advice"`
}

func (h *Handler) LogActivity(w http.ResponseWriter, r *http.Request) {
	var req activityRequest
	if err := util.DecodeJSON(r, &req); err != nil {
		util.WriteBadRequest(w, "Format permintaan tidak valid.")
		return
	}
	err := h.store.UpsertActivityLogTx(r.Context(), db.UpsertActivityLogParams{
		UserID:          h.currentUser(r).ID,
		LogDate:         today(),
		DidActivity:     req.DidActivity,
		PerDoctorAdvice: req.PerDoctorAdvice,
	})
	if err != nil {
		util.WriteInternalError(w, "Gagal menyimpan catatan.")
		return
	}
	util.WriteSuccess(w, "Catatan aktivitas fisik tersimpan.", nil)
}

func (h *Handler) ListActivity(w http.ResponseWriter, r *http.Request) {
	from, to := h.rangeParams(r)
	rows, err := h.store.ListActivityLogs(r.Context(), db.ListActivityLogsParams{
		UserID: h.currentUser(r).ID, FromLogDate: from, ToLogDate: to,
	})
	if err != nil {
		util.WriteInternalError(w, "Gagal memuat data.")
		return
	}
	util.WriteSuccess(w, "", rows)
}

// --- Diet ---

type dietRequest struct {
	PerDoctorAdvice bool `json:"per_doctor_advice"`
	OnSchedule      bool `json:"on_schedule"`
}

func (h *Handler) LogDiet(w http.ResponseWriter, r *http.Request) {
	var req dietRequest
	if err := util.DecodeJSON(r, &req); err != nil {
		util.WriteBadRequest(w, "Format permintaan tidak valid.")
		return
	}
	err := h.store.UpsertDietLogTx(r.Context(), db.UpsertDietLogParams{
		UserID:          h.currentUser(r).ID,
		LogDate:         today(),
		PerDoctorAdvice: req.PerDoctorAdvice,
		OnSchedule:      req.OnSchedule,
	})
	if err != nil {
		util.WriteInternalError(w, "Gagal menyimpan catatan.")
		return
	}
	util.WriteSuccess(w, "Catatan diet tersimpan.", nil)
}

func (h *Handler) ListDiet(w http.ResponseWriter, r *http.Request) {
	from, to := h.rangeParams(r)
	rows, err := h.store.ListDietLogs(r.Context(), db.ListDietLogsParams{
		UserID: h.currentUser(r).ID, FromLogDate: from, ToLogDate: to,
	})
	if err != nil {
		util.WriteInternalError(w, "Gagal memuat data.")
		return
	}
	util.WriteSuccess(w, "", rows)
}

// --- Glucose ---

type glucoseRequest struct {
	Timing    string `json:"timing"` // before_meal | after_meal
	ValueMgdl int32  `json:"value_mgdl"`
}

func (h *Handler) LogGlucose(w http.ResponseWriter, r *http.Request) {
	var req glucoseRequest
	if err := util.DecodeJSON(r, &req); err != nil {
		util.WriteBadRequest(w, "Format permintaan tidak valid.")
		return
	}
	timing := db.GlucoseLogTimingBeforeMeal
	if req.Timing == "after_meal" {
		timing = db.GlucoseLogTimingAfterMeal
	}
	if req.ValueMgdl <= 0 {
		util.WriteBadRequest(w, "Nilai gula darah tidak valid.")
		return
	}
	_, err := h.store.CreateGlucoseLogTx(r.Context(), db.CreateGlucoseLogParams{
		UserID:     h.currentUser(r).ID,
		MeasuredAt: time.Now(),
		Timing:     timing,
		ValueMgdl:  req.ValueMgdl,
	})
	if err != nil {
		util.WriteInternalError(w, "Gagal menyimpan catatan.")
		return
	}
	util.WriteSuccess(w, "Catatan gula darah tersimpan.", nil)
}

func (h *Handler) ListGlucose(w http.ResponseWriter, r *http.Request) {
	from, to := h.rangeParams(r)
	rows, err := h.store.ListGlucoseLogs(r.Context(), db.ListGlucoseLogsParams{
		UserID:         h.currentUser(r).ID,
		FromMeasuredAt: from,
		ToMeasuredAt:   to.Add(24*time.Hour - time.Second),
	})
	if err != nil {
		util.WriteInternalError(w, "Gagal memuat data.")
		return
	}
	util.WriteSuccess(w, "", rows)
}

// rangeParams reads ?from=&to= (YYYY-MM-DD), defaulting to the last 30 days.
func (h *Handler) rangeParams(r *http.Request) (time.Time, time.Time) {
	to := parseDate(r.URL.Query().Get("to"), today())
	from := parseDate(r.URL.Query().Get("from"), to.AddDate(0, 0, -30))
	return from, to
}
