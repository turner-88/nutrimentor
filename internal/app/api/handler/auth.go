package handler

import (
	"database/sql"
	"errors"
	"net/http"
	"strings"

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
	if u.GroupID.Valid {
		d.GroupID = &u.GroupID.Int32
	}
	if u.StudyArm.Valid {
		d.StudyArm = string(u.StudyArm.UserStudyArm)
	}
	return d
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
