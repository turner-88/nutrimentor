// Package notify orchestrates domain push notifications: it resolves the
// recipient audience from the database, sends via FCM, and prunes device
// tokens that FCM reports as permanently invalid.
package notify

import (
	"context"
	"log"
	"time"

	"github.com/remorac/sebaya-app/internal/database/store"
	"github.com/remorac/sebaya-app/internal/shared/fcm"
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

// DailyReminder notifies active patients who have not logged their medication
// today, prompting them to record their daily logs.
func (n *Service) DailyReminder(ctx context.Context) {
	day := time.Now()
	tokens, err := n.store.ListPatientTokensNeedingReminder(ctx, day)
	if err != nil {
		log.Printf("notify: list reminder audience: %v", err)
		return
	}
	n.send(ctx, tokens,
		"Jangan lupa mencatat hari ini",
		"Catat pengingat obat, aktivitas, diet, dan gula darah Anda di aplikasi SebayaDM.",
		map[string]string{"type": "reminder"})
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
