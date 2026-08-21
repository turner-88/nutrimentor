package main

import (
	"context"
	"io/fs"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/go-chi/chi/v5"
	chimw "github.com/go-chi/chi/v5/middleware"

	assets "github.com/remorac/nutrimentor-app"
	"github.com/remorac/nutrimentor-app/internal/app/admin"
	"github.com/remorac/nutrimentor-app/internal/app/api"
	"github.com/remorac/nutrimentor-app/internal/database"
	"github.com/remorac/nutrimentor-app/internal/database/store"
	"github.com/remorac/nutrimentor-app/internal/shared/config"
	"github.com/remorac/nutrimentor-app/internal/shared/fcm"
	mw "github.com/remorac/nutrimentor-app/internal/shared/middleware"
	"github.com/remorac/nutrimentor-app/internal/shared/notify"
	"github.com/remorac/nutrimentor-app/internal/shared/scheduler"
)

func main() {
	cfg := config.Load()

	db, err := database.Open(&cfg.DB)
	if err != nil {
		log.Fatalf("Failed to open database: %v", err)
	}
	defer db.Close()
	if err := db.Ping(); err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
	log.Println("Database connected")

	s := store.New(db)

	// Push notifications (FCM). Degrades to a logging no-op when unconfigured.
	sender, err := fcm.New(cfg.FCM)
	if err != nil {
		log.Fatalf("Failed to init FCM: %v", err)
	}
	notifier := notify.New(s, sender)

	// Daily log-reminder scheduler; stops on shutdown via rootCtx.
	rootCtx, rootCancel := context.WithCancel(context.Background())
	defer rootCancel()
	scheduler.StartReminders(rootCtx, cfg, notifier)

	r := chi.NewRouter()
	r.Use(mw.Logger)
	r.Use(chimw.Compress(5))
	r.Use(chimw.RedirectSlashes)

	// Uploaded files (runtime, on disk) — 1 day cache.
	r.With(mw.CacheControl(86400)).Handle("/static/uploads/*",
		http.StripPrefix("/static/uploads/", http.FileServer(http.Dir("static/uploads"))))

	// Embedded static assets (CSS/JS) — 7 day cache.
	staticFS, _ := fs.Sub(assets.StaticFS, "static")
	r.With(mw.CacheControl(604800)).Handle("/static/*",
		http.StripPrefix("/static/", http.FileServer(http.FS(staticFS))))

	// Landing → admin login.
	r.Get("/", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/admin/login", http.StatusFound)
	})

	r.Mount("/admin", admin.Routes(cfg, s, notifier))
	r.Mount("/api", api.Routes(cfg, s))

	srv := &http.Server{Addr: cfg.ServerAddr(), Handler: r}
	go func() {
		log.Printf("Server starting on http://%s", cfg.ServerAddr())
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Server failed to start: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	log.Println("Shutting down server...")
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := srv.Shutdown(ctx); err != nil {
		log.Fatalf("Server forced to shutdown: %v", err)
	}
	log.Println("Server exited gracefully")
}
