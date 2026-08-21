package handler

import (
	"fmt"
	"html/template"
	"net/http"
	"strings"
	"time"

	assets "github.com/remorac/nutrimentor-app"
	"github.com/remorac/nutrimentor-app/internal/database/store"
	"github.com/remorac/nutrimentor-app/internal/shared/config"
	mw "github.com/remorac/nutrimentor-app/internal/shared/middleware"
	"github.com/remorac/nutrimentor-app/internal/shared/notify"
)

// Handler holds dependencies and pre-parsed templates for the admin panel.
type Handler struct {
	config *config.Config
	store  *store.Store
	notify *notify.Service

	// page templates rendered inside the base (authenticated) layout
	pages map[string]*template.Template
	// guest templates rendered inside the guest layout (login)
	guest map[string]*template.Template
}

var funcs = template.FuncMap{
	"add": func(a, b int) int { return a + b },
	"sub": func(a, b int) int { return a - b },
	"seq": func(n int) []int {
		s := make([]int, n)
		for i := range s {
			s[i] = i + 1
		}
		return s
	},
	"pct": func(a, b int64) int {
		if b == 0 {
			return 0
		}
		return int(a * 100 / b)
	},
	"ratio": func(a, b int) int {
		if b == 0 {
			return 0
		}
		return a * 100 / b
	},
	"ymd":      func(t time.Time) string { return t.Format("2006-01-02") },
	"md":       func(t time.Time) string { return t.Format("02 Jan") },
	"datetime": func(t time.Time) string { return t.Format("2006-01-02 15:04") },
	"initials": initials,
	"timing": func(v any) string {
		switch fmt.Sprintf("%v", v) {
		case "before_meal":
			return "Sebelum makan"
		case "after_meal":
			return "Sesudah makan"
		default:
			return fmt.Sprintf("%v", v)
		}
	},
}

// initials returns up to two uppercase initials for a monogram avatar.
func initials(name string) string {
	parts := strings.Fields(name)
	if len(parts) == 0 {
		return "?"
	}
	first := []rune(parts[0])
	out := strings.ToUpper(string(first[0]))
	if len(parts) > 1 {
		last := []rune(parts[len(parts)-1])
		out += strings.ToUpper(string(last[0]))
	}
	return out
}

// New parses all admin templates and returns a Handler.
func New(cfg *config.Config, s *store.Store, notifier *notify.Service) *Handler {
	base := "template/layouts/admin/base.html"
	guestLayout := "template/layouts/admin/guest.html"

	page := func(files ...string) *template.Template {
		all := append([]string{base}, files...)
		return template.Must(template.New("base.html").Funcs(funcs).ParseFS(assets.TemplateFS, all...))
	}
	guestPage := func(file string) *template.Template {
		return template.Must(template.New("guest.html").Funcs(funcs).ParseFS(assets.TemplateFS, guestLayout, file))
	}

	return &Handler{
		config: cfg,
		store:  s,
		notify: notifier,
		pages: map[string]*template.Template{
			"dashboard":      page("template/pages/admin/dashboard.html"),
			"patients":       page("template/pages/admin/patients.html"),
			"patient_detail": page("template/pages/admin/patient_detail.html"),
			"groups":         page("template/pages/admin/groups.html"),
			"group_detail":   page("template/pages/admin/group_detail.html"),
			"group_form":     page("template/pages/admin/group_form.html"),
			"articles":       page("template/pages/admin/articles.html"),
			"article_form":   page("template/pages/admin/article_form.html"),
			"article_detail": page("template/pages/admin/article_detail.html"),
			"monitoring":     page("template/pages/admin/monitoring.html"),
			"audit":          page("template/pages/admin/audit.html"),
		},
		guest: map[string]*template.Template{
			"login":          guestPage("template/pages/admin/login.html"),
			"reset_password": guestPage("template/pages/admin/reset_password.html"),
		},
	}
}

// base injects common template data (theme, year, active nav, auth user).
func (h *Handler) base(r *http.Request, data map[string]any) map[string]any {
	if data == nil {
		data = map[string]any{}
	}
	data["Theme"] = h.config.AppTheme
	data["Year"] = time.Now().Year()
	if u := mw.GetUserFromContext(r.Context()); u != nil {
		data["AuthUser"] = u
	}
	return data
}

// render executes an authenticated page inside the base layout.
func (h *Handler) render(w http.ResponseWriter, r *http.Request, page string, data map[string]any) {
	tmpl, ok := h.pages[page]
	if !ok {
		http.Error(w, "unknown page: "+page, http.StatusInternalServerError)
		return
	}
	if err := tmpl.ExecuteTemplate(w, "base.html", h.base(r, data)); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}
}

// renderGuest executes a guest page (login) inside the guest layout.
func (h *Handler) renderGuest(w http.ResponseWriter, page string, data map[string]any) {
	if data == nil {
		data = map[string]any{}
	}
	data["Theme"] = h.config.AppTheme
	data["Year"] = time.Now().Year()
	if err := h.guest[page].ExecuteTemplate(w, "guest.html", data); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}
}
