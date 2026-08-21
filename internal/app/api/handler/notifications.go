package handler

import (
	"net/http"
	"strings"

	db "github.com/remorac/nutrimentor-app/internal/database/sqlc"
	"github.com/remorac/nutrimentor-app/internal/shared/util"
)

type deviceTokenRequest struct {
	Token    string `json:"token"`
	Platform string `json:"platform"`
}

// RegisterDeviceToken associates an FCM device token with the current user.
// Re-registering an existing token re-points it to the current user.
func (h *Handler) RegisterDeviceToken(w http.ResponseWriter, r *http.Request) {
	var req deviceTokenRequest
	if err := util.DecodeJSON(r, &req); err != nil {
		util.WriteBadRequest(w, "Format permintaan tidak valid.")
		return
	}
	req.Token = strings.TrimSpace(req.Token)
	if req.Token == "" {
		util.WriteBadRequest(w, "Token perangkat wajib diisi.")
		return
	}
	platform := strings.TrimSpace(req.Platform)
	if platform == "" {
		platform = "android"
	}

	uid := h.currentUser(r).ID
	if err := h.store.UpsertDeviceTokenTx(r.Context(), db.UpsertDeviceTokenParams{
		UserID:   uid,
		Token:    req.Token,
		Platform: platform,
	}, uid); err != nil {
		util.WriteInternalError(w, "Gagal mendaftarkan perangkat.")
		return
	}
	util.WriteSuccess(w, "Perangkat terdaftar untuk notifikasi.", nil)
}

// UnregisterDeviceToken removes an FCM device token (e.g. on logout).
func (h *Handler) UnregisterDeviceToken(w http.ResponseWriter, r *http.Request) {
	var req deviceTokenRequest
	if err := util.DecodeJSON(r, &req); err != nil {
		util.WriteBadRequest(w, "Format permintaan tidak valid.")
		return
	}
	req.Token = strings.TrimSpace(req.Token)
	if req.Token == "" {
		util.WriteBadRequest(w, "Token perangkat wajib diisi.")
		return
	}
	uid := h.currentUser(r).ID
	if err := h.store.DeleteDeviceTokenTx(r.Context(), req.Token, uid); err != nil {
		util.WriteInternalError(w, "Gagal menghapus perangkat.")
		return
	}
	util.WriteSuccess(w, "Perangkat dihapus dari notifikasi.", nil)
}
