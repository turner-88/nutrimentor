package handler

import (
	"log"
	"net/http"
	"strings"

	"github.com/remorac/sebaya-app/internal/shared/model"
	"github.com/remorac/sebaya-app/internal/shared/util"
)

// LoginPage renders the admin login form.
func (h *Handler) LoginPage(w http.ResponseWriter, r *http.Request) {
	if c, err := r.Cookie("admin_token"); err == nil {
		if _, err := util.ValidateToken(c.Value, h.config.JWT.SecretKey); err == nil {
			http.Redirect(w, r, "/admin/dashboard", http.StatusFound)
			return
		}
	}
	h.renderGuest(w, "login", map[string]any{"Title": "Admin Login — SebayaDM"})
}

// Login processes the admin login form.
func (h *Handler) Login(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseForm(); err != nil {
		http.Error(w, "Bad request", http.StatusBadRequest)
		return
	}
	username := strings.TrimSpace(r.FormValue("username"))
	password := r.FormValue("password")

	fail := func(msg string) {
		w.WriteHeader(http.StatusUnprocessableEntity)
		h.renderGuest(w, "login", map[string]any{
			"Title": "Admin Login — SebayaDM", "Error": msg, "Username": username,
		})
	}

	if username == "" || password == "" {
		fail("Username dan password wajib diisi.")
		return
	}

	u, err := h.store.GetUserByUsername(r.Context(), username)
	if err != nil || !util.CheckPassword(u.PasswordHash, password) {
		fail("Username atau password salah.")
		return
	}
	if model.UserRole(u.Role) != model.UserRoleAdmin {
		fail("Akun ini tidak memiliki akses admin.")
		return
	}

	token, err := util.GenerateToken(&model.User{
		ID: u.ID, Username: u.Username, Role: model.UserRole(u.Role),
	}, &util.JWTConfig{
		SecretKey:       h.config.JWT.SecretKey,
		ExpirationHours: h.config.JWT.ExpirationHours,
	})
	if err != nil {
		log.Printf("[admin/Login] token error: %v", err)
		fail("Terjadi kesalahan. Silakan coba lagi.")
		return
	}
	http.SetCookie(w, &http.Cookie{
		Name:     "admin_token",
		Value:    token,
		Path:     "/admin",
		HttpOnly: true,
		Secure:   h.config.IsProduction(),
		SameSite: http.SameSiteLaxMode,
		MaxAge:   h.config.JWT.ExpirationHours * 3600,
	})
	http.Redirect(w, r, "/admin/dashboard", http.StatusFound)
}

// Logout clears the admin cookie.
func (h *Handler) Logout(w http.ResponseWriter, r *http.Request) {
	http.SetCookie(w, &http.Cookie{Name: "admin_token", Value: "", Path: "/admin", MaxAge: -1})
	http.Redirect(w, r, "/admin/login", http.StatusFound)
}
