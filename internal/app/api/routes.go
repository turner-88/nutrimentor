package api

import (
	"github.com/go-chi/chi/v5"

	"github.com/remorac/sebaya-app/internal/app/api/handler"
	"github.com/remorac/sebaya-app/internal/database/store"
	"github.com/remorac/sebaya-app/internal/shared/config"
	mw "github.com/remorac/sebaya-app/internal/shared/middleware"
)

// Routes returns the JSON API subsystem router (consumed by the Android app).
func Routes(cfg *config.Config, s *store.Store) chi.Router {
	r := chi.NewRouter()
	h := handler.New(cfg, s)

	r.Get("/health", h.Health)

	// Public auth
	r.Post("/auth/register", h.Register)
	r.Post("/auth/login", h.Login)
	r.Post("/auth/forgot-password", h.ForgotPassword)

	// Public education (read-only)
	r.Get("/education", h.ListEducation)
	r.Get("/education/{slug}", h.GetEducation)

	// Protected (Bearer JWT required)
	r.Group(func(r chi.Router) {
		r.Use(mw.RequireAuth(&mw.AuthConfig{JWTSecret: cfg.JWT.SecretKey}))

		r.Get("/me", h.Me)
		r.Put("/me", h.UpdateMe)
		r.Post("/me/password", h.ChangePassword)
		r.Get("/dashboard", h.Dashboard)

		r.Post("/logs/medication", h.LogMedication)
		r.Get("/logs/medication", h.ListMedication)
		r.Post("/logs/activity", h.LogActivity)
		r.Get("/logs/activity", h.ListActivity)
		r.Post("/logs/diet", h.LogDiet)
		r.Get("/logs/diet", h.ListDiet)
		r.Post("/logs/glucose", h.LogGlucose)
		r.Get("/logs/glucose", h.ListGlucose)

		r.Get("/leaderboard", h.Leaderboard)

		// Push notification device-token registration.
		r.Post("/me/device-token", h.RegisterDeviceToken)
		r.Delete("/me/device-token", h.UnregisterDeviceToken)
	})

	return r
}
