package middleware

import (
	"context"
	"net/http"
	"strings"

	"github.com/remorac/nutrimentor-app/internal/shared/model"
	"github.com/remorac/nutrimentor-app/internal/shared/util"
)

type contextKey string

const UserContextKey contextKey = "user"

// AuthConfig configures the RequireAuth middleware.
type AuthConfig struct {
	JWTSecret   string
	RedirectURL string // if set, redirect here on failure instead of 401 JSON
	CookieName  string // cookie to read token from; defaults to "auth_token"
}

// RequireAuth validates a JWT (Authorization: Bearer header first, then cookie)
// and places the resulting *model.User in the request context.
func RequireAuth(cfg *AuthConfig) func(http.Handler) http.Handler {
	cookieName := cfg.CookieName
	if cookieName == "" {
		cookieName = "auth_token"
	}
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			tokenString := ""
			if h := r.Header.Get("Authorization"); h != "" {
				if parts := strings.SplitN(h, " ", 2); len(parts) == 2 && parts[0] == "Bearer" {
					tokenString = parts[1]
				}
			}
			if tokenString == "" {
				if c, err := r.Cookie(cookieName); err == nil {
					tokenString = c.Value
				}
			}
			if tokenString == "" {
				authFail(w, r, cfg.RedirectURL, "Missing authorization")
				return
			}
			claims, err := util.ValidateToken(tokenString, cfg.JWTSecret)
			if err != nil {
				http.SetCookie(w, &http.Cookie{Name: cookieName, Value: "", Path: "/", MaxAge: -1})
				authFail(w, r, cfg.RedirectURL, "Invalid or expired token")
				return
			}
			user := &model.User{
				ID:       int32(claims.UserID),
				Username: claims.Username,
				Role:     claims.Role,
			}
			ctx := context.WithValue(r.Context(), UserContextKey, user)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

func authFail(w http.ResponseWriter, r *http.Request, redirectURL, msg string) {
	if redirectURL != "" {
		http.Redirect(w, r, redirectURL, http.StatusFound)
		return
	}
	util.WriteUnauthorized(w, msg)
}

// GetUserFromContext returns the authenticated user, or nil.
func GetUserFromContext(ctx context.Context) *model.User {
	u, ok := ctx.Value(UserContextKey).(*model.User)
	if !ok {
		return nil
	}
	return u
}

// RequireRole ensures the authenticated user has the given role (JSON 403 on failure).
func RequireRole(role model.UserRole) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			u := GetUserFromContext(r.Context())
			if u == nil {
				util.WriteUnauthorized(w, "User not authenticated")
				return
			}
			if u.Role != role {
				util.WriteForbidden(w, "Insufficient permissions")
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}
