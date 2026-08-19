package admin

import (
	"github.com/go-chi/chi/v5"

	"github.com/remorac/sebaya-app/internal/app/admin/handler"
	adminMW "github.com/remorac/sebaya-app/internal/app/admin/middleware"
	"github.com/remorac/sebaya-app/internal/database/store"
	"github.com/remorac/sebaya-app/internal/shared/config"
	mw "github.com/remorac/sebaya-app/internal/shared/middleware"
	"github.com/remorac/sebaya-app/internal/shared/notify"
)

// Routes returns the admin panel subsystem router.
func Routes(cfg *config.Config, s *store.Store, notifier *notify.Service) chi.Router {
	r := chi.NewRouter()
	h := handler.New(cfg, s, notifier)

	// Public auth routes
	r.Get("/login", h.LoginPage)
	r.Post("/login", h.Login)
	r.Get("/logout", h.Logout)
	r.Get("/reset-password", h.ShowResetPassword)
	r.Post("/reset-password", h.SubmitResetPassword)

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
		r.Post("/patients/{id}/edit", h.UpdatePatient)
		r.Post("/patients/{id}/password", h.ResetPatientPassword)
		r.Post("/patients/{id}/active", h.ToggleActive)

		// Peer groups
		r.Get("/groups", h.ListGroups)
		r.Get("/groups/new", h.NewGroupForm)
		r.Post("/groups/new", h.CreateGroup)
		r.Get("/groups/{id}", h.GroupDetail)
		r.Get("/groups/{id}/edit", h.EditGroupForm)
		r.Post("/groups/{id}/edit", h.UpdateGroup)
		r.Post("/groups/{id}/delete", h.DeleteGroup)

		// Education articles
		r.Get("/articles", h.ListArticles)
		r.Get("/articles/new", h.NewArticleForm)
		r.Post("/articles/new", h.SaveArticle)
		r.Get("/articles/{id}", h.ArticleDetail)
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
