package handler

import (
	"net/http"
	"strings"

	db "github.com/remorac/sebaya-app/internal/database/sqlc"
	"github.com/remorac/sebaya-app/internal/shared/util"
)

// ShowResetPassword renders the public password-reset form for a valid token.
func (h *Handler) ShowResetPassword(w http.ResponseWriter, r *http.Request) {
	token := strings.TrimSpace(r.URL.Query().Get("token"))
	data := map[string]any{"Title": "Reset Password — SebayaDM", "Token": token}
	if _, err := h.store.GetValidResetToken(r.Context(), util.HashToken(token)); err != nil {
		data["Invalid"] = true
	}
	h.renderGuest(w, "reset_password", data)
}

// SubmitResetPassword validates the token and sets the new password.
func (h *Handler) SubmitResetPassword(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseForm(); err != nil {
		http.Error(w, "Bad request", http.StatusBadRequest)
		return
	}
	token := strings.TrimSpace(r.FormValue("token"))
	password := r.FormValue("password")
	confirm := r.FormValue("confirm")

	fail := func(msg string) {
		w.WriteHeader(http.StatusUnprocessableEntity)
		h.renderGuest(w, "reset_password", map[string]any{
			"Title": "Reset Password — SebayaDM", "Token": token, "Error": msg,
		})
	}

	prt, err := h.store.GetValidResetToken(r.Context(), util.HashToken(token))
	if err != nil {
		h.renderGuest(w, "reset_password", map[string]any{
			"Title": "Reset Password — SebayaDM", "Invalid": true,
		})
		return
	}
	if password != confirm {
		fail("Konfirmasi password tidak cocok.")
		return
	}
	if err := util.ValidatePassword(password); err != nil {
		fail(err.Error())
		return
	}
	hash, err := util.HashPassword(password)
	if err != nil {
		fail("Terjadi kesalahan. Silakan coba lagi.")
		return
	}
	if err := h.store.UpdateUserPasswordTx(r.Context(), db.UpdateUserPasswordParams{PasswordHash: hash, ID: prt.UserID}, prt.UserID); err != nil {
		fail("Terjadi kesalahan. Silakan coba lagi.")
		return
	}
	if err := h.store.MarkResetTokenUsed(r.Context(), prt.ID); err != nil {
		fail("Terjadi kesalahan. Silakan coba lagi.")
		return
	}
	h.renderGuest(w, "reset_password", map[string]any{
		"Title": "Reset Password — SebayaDM", "Success": true,
	})
}
