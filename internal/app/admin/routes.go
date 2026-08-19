package admin

import (
	"github.com/go-chi/chi/v5"

	"github.com/remorac/sebaya-app/internal/app/admin/handler"
	adminMW "github.com/remorac/sebaya-app/internal/app/admin/middleware"
	"github.com/remorac/sebaya-app/internal/database/store"
	"github.com/remorac/sebaya-app/internal/shared/config"
	mw "github.com/remorac/sebaya-app/internal/shared/middleware"
)

// Routes returns the admin panel subsystem router.
func Routes(cfg *config.Config, s *store.Store) chi.Router {
	r := chi.NewRouter()
	h := handler.New(cfg, s)

	// Public auth routes
	r.Get("/login", h.LoginPage)
	r.Post("/login", h.Login)
	r.Get("/logout", h.Logout)

	// Protected routes — valid admin JWT required
	r.Group(func(r chi.Router) {
		r.Use(mw.RequireAuth(&mw.AuthConfig{
			JWTSecret:   cfg.JWT.SecretKey,
			RedirectURL: "/admin/login",
			CookieName:  "admin_token",
		}))
		r.Use(adminMW.RequireAdmin)

		r.Get("/", h.Dashboard)
		r.Get("/dashboard", h.Dashboard)

		// Patients (responden)
		r.Get("/patients", h.ListPatients)
		r.Get("/patients/{id}", h.PatientDetail)
		r.Post("/patients/{id}/group", h.AssignGroup)
		r.Post("/patients/{id}/password", h.ResetPatientPassword)
		r.Post("/patients/{id}/active", h.ToggleActive)

		// Peer groups
		r.Get("/groups", h.ListGroups)
		r.Post("/groups", h.CreateGroup)
		r.Post("/groups/{id}", h.UpdateGroup)
		r.Post("/groups/{id}/delete", h.DeleteGroup)

		// Education articles
		r.Get("/articles", h.ListArticles)
		r.Get("/articles/new", h.NewArticleForm)
		r.Post("/articles/new", h.SaveArticle)
		r.Get("/articles/{id}/edit", h.EditArticleForm)
		r.Post("/articles/{id}/edit", h.SaveArticle)
		r.Post("/articles/{id}/delete", h.DeleteArticle)

		// Monitoring & reports
		r.Get("/monitoring", h.Monitoring)
		r.Get("/monitoring/export.csv", h.ExportCSV)

		// Audit trail
		r.Get("/audit", h.ListAudit)
	})

	return r
}
