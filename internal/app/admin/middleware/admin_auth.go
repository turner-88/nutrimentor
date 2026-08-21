package middleware

import (
	"net/http"

	mw "github.com/remorac/nutrimentor-app/internal/shared/middleware"
	"github.com/remorac/nutrimentor-app/internal/shared/model"
)

// RequireAdmin ensures the authenticated user has the admin role, redirecting
// to the login page otherwise.
func RequireAdmin(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		u := mw.GetUserFromContext(r.Context())
		if u == nil || u.Role != model.UserRoleAdmin {
			http.Redirect(w, r, "/admin/login", http.StatusFound)
			return
		}
		next.ServeHTTP(w, r)
	})
}
