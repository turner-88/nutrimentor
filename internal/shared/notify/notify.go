// Package notify orchestrates domain push notifications: it resolves the
// recipient audience from the database, sends via FCM, and prunes device
// tokens that FCM reports as permanently invalid.
package notify

import (
	"context"
	"log"
	"time"

	db "github.com/remorac/nutrimentor-app/internal/database/sqlc"
	"github.com/remorac/nutrimentor-app/internal/database/store"
	"github.com/remorac/nutrimentor-app/internal/shared/fcm"
)

type Service struct {
	store  *store.Store
	sender *fcm.Sender
}

func New(s *store.Store, sender *fcm.Sender) *Service {
	return &Service{store: s, sender: sender}
}

// NewArticle notifies all active patients that a new education article was
// published. Safe to call from a goroutine; it logs its own errors.
func (n *Service) NewArticle(ctx context.Context, title, slug string) {
	tokens, err := n.store.ListActivePatientTokens(ctx)
	if err != nil {
		log.Printf("notify: list article audience: %v", err)
		return
	}
	n.send(ctx, tokens, "Artikel edukasi baru", title, map[string]string{
		"type": "article",
		"slug": slug,
	})
}

// DailyLogReminder notifies active patients who have not completed all of today's
// daily logs (medication, activity, diet, or glucose), prompting them to record.
func (n *Service) DailyLogReminder(ctx context.Context) {
	tokens, err := n.store.ListPatientTokensNeedingDailyLog(ctx, db.ListPatientTokensNeedingDailyLogParams{Day: time.Now()})
	if err != nil {
		log.Printf("notify: list daily-log reminder audience: %v", err)
		return
	}
	n.send(ctx, tokens,
		"Jangan lupa mencatat hari ini",
		"Catat pengingat obat, aktivitas, diet, dan gula darah Anda di aplikasi NutriMentor.",
		map[string]string{"type": "reminder"})
}

// ArticleReminder notifies active patients who still have unread published
// articles, prompting them to finish reading.
func (n *Service) ArticleReminder(ctx context.Context) {
	tokens, err := n.store.ListPatientTokensWithUnreadArticles(ctx)
	if err != nil {
		log.Printf("notify: list article-reminder audience: %v", err)
		return
	}
	n.send(ctx, tokens,
		"Masih ada artikel yang belum dibaca",
		"Lanjutkan membaca artikel edukasi diabetes Anda di aplikasi NutriMentor.",
		map[string]string{"type": "reminder_article"})
}

// send delivers to FCM and prunes any tokens reported as invalid.
func (n *Service) send(ctx context.Context, tokens []string, title, body string, data map[string]string) {
	invalid, err := n.sender.Send(ctx, tokens, title, body, data)
	if err != nil {
		log.Printf("notify: send %q: %v", title, err)
		return
	}
	for _, tok := range invalid {
		if err := n.store.DeleteDeviceToken(ctx, tok); err != nil {
			log.Printf("notify: prune invalid token: %v", err)
		}
	}
}
