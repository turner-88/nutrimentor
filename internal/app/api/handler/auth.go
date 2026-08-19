package handler

import (
	"database/sql"
	"errors"
	"fmt"
	"net/http"
	"net/mail"
	"strings"
	"time"

	db "github.com/remorac/sebaya-app/internal/database/sqlc"
	"github.com/remorac/sebaya-app/internal/shared/model"
	"github.com/remorac/sebaya-app/internal/shared/util"
)

type registerRequest struct {
	NamaLengkap  string `json:"nama_lengkap"`
	Usia         int32  `json:"usia"`
	JenisKelamin string `json:"jenis_kelamin"` // "L" or "P"
	Pendidikan   string `json:"pendidikan"`
	Pekerjaan    string `json:"pekerjaan"`
	Username     string `json:"username"`
	Password     string `json:"password"`
	Email        string `json:"email"`
}

type loginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

// userDTO is the public shape of a user returned to the app.
type userDTO struct {
	ID           int32  `json:"id"`
	Role         string `json:"role"`
	NamaLengkap  string `json:"nama_lengkap"`
	Usia         int32  `json:"usia"`
	JenisKelamin string `json:"jenis_kelamin"`
	Pendidikan   string `json:"pendidikan"`
	Pekerjaan    string `json:"pekerjaan"`
	Username     string `json:"username"`
	Email        string `json:"email"`
	GroupID      *int32 `json:"group_id"`
	StudyArm     string `json:"study_arm"`
}

func toUserDTO(u db.User) userDTO {
	d := userDTO{
		ID:           u.ID,
		Role:         string(u.Role),
		NamaLengkap:  u.NamaLengkap,
		Usia:         u.Usia,
		JenisKelamin: string(u.JenisKelamin),
		Pendidikan:   u.Pendidikan,
		Pekerjaan:    u.Pekerjaan,
		Username:     u.Username,
	}
	if u.Email.Valid {
		d.Email = u.Email.String
	}
	if u.GroupID.Valid {
		d.GroupID = &u.GroupID.Int32
	}
	if u.StudyArm.Valid {
		d.StudyArm = string(u.StudyArm.UserStudyArm)
	}
	return d
}

// emailToNull normalizes a user-supplied email into a nullable column value.
// Empty string becomes NULL so it does not collide on the unique index.
func emailToNull(s string) sql.NullString {
	s = strings.TrimSpace(s)
	if s == "" {
		return sql.NullString{}
	}
	return sql.NullString{String: s, Valid: true}
}

// Register creates a new patient account and returns a JWT.
func (h *Handler) Register(w http.ResponseWriter, r *http.Request) {
	var req registerRequest
	if err := util.DecodeJSON(r, &req); err != nil {
		util.WriteBadRequest(w, "Format permintaan tidak valid.")
		return
	}
	req.Username = strings.TrimSpace(req.Username)
	req.NamaLengkap = strings.TrimSpace(req.NamaLengkap)

	if req.NamaLengkap == "" || req.Username == "" {
		util.WriteBadRequest(w, "Nama lengkap dan username wajib diisi.")
		return
	}
	if err := util.ValidatePassword(req.Password); err != nil {
		util.WriteBadRequest(w, err.Error())
		return
	}
	jk := db.UserJenisKelaminL
	if strings.EqualFold(req.JenisKelamin, "P") {
		jk = db.UserJenisKelaminP
	}

	if _, err := h.store.GetUserByUsername(r.Context(), req.Username); err == nil {
		util.WriteError(w, http.StatusConflict, "Username sudah digunakan.")
		return
	} else if !errors.Is(err, sql.ErrNoRows) {
		util.WriteInternalError(w, "Terjadi kesalahan pada server.")
		return
	}

	email := emailToNull(req.Email)
	if email.Valid {
		if _, err := mail.ParseAddress(email.String); err != nil {
			util.WriteBadRequest(w, "Format email tidak valid.")
			return
		}
		if _, err := h.store.GetUserByEmail(r.Context(), email); err == nil {
			util.WriteError(w, http.StatusConflict, "Email sudah digunakan.")
			return
		} else if !errors.Is(err, sql.ErrNoRows) {
			util.WriteInternalError(w, "Terjadi kesalahan pada server.")
			return
		}
	}

	hash, err := util.HashPassword(req.Password)
	if err != nil {
		util.WriteInternalError(w, "Terjadi kesalahan pada server.")
		return
	}

	id, err := h.store.CreateUserTx(r.Context(), db.CreateUserParams{
		Role:         db.UserRolePatient,
		NamaLengkap:  req.NamaLengkap,
		Usia:         req.Usia,
		JenisKelamin: jk,
		Pendidikan:   req.Pendidikan,
		Pekerjaan:    req.Pekerjaan,
		Username:     req.Username,
		PasswordHash: hash,
		GroupID:      sql.NullInt32{},
		StudyArm:     db.NullUserStudyArm{},
		Email:        email,
	}, 0)
	if err != nil {
		util.WriteInternalError(w, "Gagal membuat akun.")
		return
	}

	u, err := h.store.GetUserByID(r.Context(), id)
	if err != nil {
		util.WriteInternalError(w, "Terjadi kesalahan pada server.")
		return
	}
	h.issueToken(w, u, "Registrasi berhasil.")
}

// Login authenticates a user and returns a JWT.
func (h *Handler) Login(w http.ResponseWriter, r *http.Request) {
	var req loginRequest
	if err := util.DecodeJSON(r, &req); err != nil {
		util.WriteBadRequest(w, "Format permintaan tidak valid.")
		return
	}
	req.Username = strings.TrimSpace(req.Username)

	u, err := h.store.GetUserByUsername(r.Context(), req.Username)
	if err != nil || !util.CheckPassword(u.PasswordHash, req.Password) {
		util.WriteUnauthorized(w, "Username atau password salah.")
		return
	}
	if !u.IsActive {
		util.WriteForbidden(w, "Akun Anda dinonaktifkan.")
		return
	}
	h.issueToken(w, u, "Login berhasil.")
}

// Me returns the current authenticated user's profile.
func (h *Handler) Me(w http.ResponseWriter, r *http.Request) {
	u, err := h.store.GetUserByID(r.Context(), h.currentUser(r).ID)
	if err != nil {
		util.WriteNotFound(w, "Pengguna tidak ditemukan.")
		return
	}
	util.WriteSuccess(w, "", toUserDTO(u))
}

type updateProfileRequest struct {
	NamaLengkap  string `json:"nama_lengkap"`
	Usia         int32  `json:"usia"`
	JenisKelamin string `json:"jenis_kelamin"`
	Pendidikan   string `json:"pendidikan"`
	Pekerjaan    string `json:"pekerjaan"`
	Email        string `json:"email"`
}

// UpdateMe updates the current authenticated user's own profile.
func (h *Handler) UpdateMe(w http.ResponseWriter, r *http.Request) {
	var req updateProfileRequest
	if err := util.DecodeJSON(r, &req); err != nil {
		util.WriteBadRequest(w, "Format permintaan tidak valid.")
		return
	}
	req.NamaLengkap = strings.TrimSpace(req.NamaLengkap)
	if req.NamaLengkap == "" {
		util.WriteBadRequest(w, "Nama lengkap wajib diisi.")
		return
	}
	jk := db.UserJenisKelaminL
	if strings.EqualFold(req.JenisKelamin, "P") {
		jk = db.UserJenisKelaminP
	}

	uid := h.currentUser(r).ID
	email := emailToNull(req.Email)
	if email.Valid {
		if existing, err := h.store.GetUserByEmail(r.Context(), email); err == nil && existing.ID != uid {
			util.WriteError(w, http.StatusConflict, "Email sudah digunakan.")
			return
		} else if err != nil && !errors.Is(err, sql.ErrNoRows) {
			util.WriteInternalError(w, "Terjadi kesalahan pada server.")
			return
		}
	}

	err := h.store.UpdateUserProfileTx(r.Context(), db.UpdateUserProfileParams{
		NamaLengkap:  req.NamaLengkap,
		Usia:         req.Usia,
		JenisKelamin: jk,
		Pendidikan:   req.Pendidikan,
		Pekerjaan:    req.Pekerjaan,
		Email:        email,
		ID:           uid,
	}, uid)
	if err != nil {
		util.WriteInternalError(w, "Gagal menyimpan profil.")
		return
	}

	u, err := h.store.GetUserByID(r.Context(), uid)
	if err != nil {
		util.WriteInternalError(w, "Terjadi kesalahan pada server.")
		return
	}
	util.WriteSuccess(w, "Profil diperbarui.", toUserDTO(u))
}

type changePasswordRequest struct {
	CurrentPassword string `json:"current_password"`
	NewPassword     string `json:"new_password"`
}

// ChangePassword changes the current authenticated user's password after
// verifying their current password.
func (h *Handler) ChangePassword(w http.ResponseWriter, r *http.Request) {
	var req changePasswordRequest
	if err := util.DecodeJSON(r, &req); err != nil {
		util.WriteBadRequest(w, "Format permintaan tidak valid.")
		return
	}
	uid := h.currentUser(r).ID
	u, err := h.store.GetUserByID(r.Context(), uid)
	if err != nil {
		util.WriteInternalError(w, "Terjadi kesalahan pada server.")
		return
	}
	if !util.CheckPassword(u.PasswordHash, req.CurrentPassword) {
		util.WriteError(w, http.StatusBadRequest, "Password saat ini salah.")
		return
	}
	if err := util.ValidatePassword(req.NewPassword); err != nil {
		util.WriteBadRequest(w, err.Error())
		return
	}
	hash, err := util.HashPassword(req.NewPassword)
	if err != nil {
		util.WriteInternalError(w, "Terjadi kesalahan pada server.")
		return
	}
	if err := h.store.UpdateUserPasswordTx(r.Context(), db.UpdateUserPasswordParams{PasswordHash: hash, ID: uid}, uid); err != nil {
		util.WriteInternalError(w, "Gagal mengubah password.")
		return
	}
	util.WriteSuccess(w, "Password berhasil diubah.", nil)
}

type forgotPasswordRequest struct {
	Email string `json:"email"`
}

// ForgotPassword issues a password-reset token and emails a reset link.
// The response is always the same regardless of whether the email exists,
// to avoid leaking which accounts are registered.
func (h *Handler) ForgotPassword(w http.ResponseWriter, r *http.Request) {
	var req forgotPasswordRequest
	if err := util.DecodeJSON(r, &req); err != nil {
		util.WriteBadRequest(w, "Format permintaan tidak valid.")
		return
	}
	const okMsg = "Jika email terdaftar, tautan reset password telah dikirim."

	email := emailToNull(req.Email)
	if !email.Valid {
		util.WriteBadRequest(w, "Email wajib diisi.")
		return
	}

	u, err := h.store.GetUserByEmail(r.Context(), email)
	if err != nil || !u.IsActive {
		util.WriteSuccess(w, okMsg, nil) // do not reveal existence
		return
	}

	raw, hash, err := util.NewResetToken()
	if err != nil {
		util.WriteInternalError(w, "Terjadi kesalahan pada server.")
		return
	}
	if _, err := h.store.CreatePasswordResetToken(r.Context(), db.CreatePasswordResetTokenParams{
		UserID:    u.ID,
		TokenHash: hash,
		ExpiresAt: time.Now().Add(time.Hour),
	}); err != nil {
		util.WriteInternalError(w, "Terjadi kesalahan pada server.")
		return
	}

	link := fmt.Sprintf("%s/admin/reset-password?token=%s", strings.TrimRight(h.config.AppURL, "/"), raw)
	body := fmt.Sprintf(
		"Halo %s,\n\nKami menerima permintaan untuk mereset password akun SebayaDM Anda.\n"+
			"Klik tautan berikut untuk membuat password baru (berlaku 1 jam):\n\n%s\n\n"+
			"Jika Anda tidak meminta reset password, abaikan email ini.\n\nSalam,\nTim SebayaDM",
		u.NamaLengkap, link)
	if err := util.SendMail(h.config.SMTP, u.Email.String, "Reset Password SebayaDM", body); err != nil {
		util.WriteInternalError(w, "Gagal mengirim email.")
		return
	}
	util.WriteSuccess(w, okMsg, nil)
}

func (h *Handler) issueToken(w http.ResponseWriter, u db.User, msg string) {
	token, err := util.GenerateToken(&model.User{
		ID:       u.ID,
		Username: u.Username,
		Role:     model.UserRole(u.Role),
	}, &util.JWTConfig{
		SecretKey:       h.config.JWT.SecretKey,
		ExpirationHours: h.config.JWT.ExpirationHours,
	})
	if err != nil {
		util.WriteInternalError(w, "Gagal membuat token.")
		return
	}
	util.WriteSuccess(w, msg, map[string]any{
		"token": token,
		"user":  toUserDTO(u),
	})
}
