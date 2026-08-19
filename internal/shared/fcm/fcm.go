// Package fcm sends push notifications through Firebase Cloud Messaging.
//
// It degrades gracefully like util.SendMail: when no credentials file is
// configured the sender logs instead of dialing FCM, so development works
// without Firebase credentials.
package fcm

import (
	"context"
	"log"
	"os"

	firebase "firebase.google.com/go/v4"
	"firebase.google.com/go/v4/messaging"
	"google.golang.org/api/option"

	"github.com/remorac/sebaya-app/internal/shared/config"
)

// Sender wraps an FCM messaging client. A nil client means "not configured".
type Sender struct {
	client *messaging.Client
}

// New builds a Sender from config. When FCM.CredentialsFile is empty it returns
// a no-op sender (client == nil) that only logs; this is not an error.
func New(cfg config.FCMConfig) (*Sender, error) {
	if cfg.CredentialsFile == "" {
		log.Println("FCM not configured (FCM_CREDENTIALS_FILE empty); push notifications will be logged only")
		return &Sender{}, nil
	}

	credJSON, err := os.ReadFile(cfg.CredentialsFile)
	if err != nil {
		return nil, err
	}

	ctx := context.Background()
	opts := []option.ClientOption{option.WithCredentialsJSON(credJSON)}
	app, err := firebase.NewApp(ctx, &firebase.Config{ProjectID: cfg.ProjectID}, opts...)
	if err != nil {
		return nil, err
	}
	client, err := app.Messaging(ctx)
	if err != nil {
		return nil, err
	}
	log.Printf("FCM configured (project %q)", cfg.ProjectID)
	return &Sender{client: client}, nil
}

// Send delivers a notification to the given device tokens. It returns the
// subset of tokens FCM reports as permanently invalid (unregistered), so the
// caller can prune them from the database. When the sender is unconfigured it
// logs and returns no invalid tokens.
func (s *Sender) Send(ctx context.Context, tokens []string, title, body string, data map[string]string) (invalid []string, err error) {
	if len(tokens) == 0 {
		return nil, nil
	}
	if s.client == nil {
		log.Printf("FCM (noop) would send %q to %d device(s)", title, len(tokens))
		return nil, nil
	}

	msg := &messaging.MulticastMessage{
		Tokens: tokens,
		Notification: &messaging.Notification{
			Title: title,
			Body:  body,
		},
		Data: data,
	}

	resp, err := s.client.SendEachForMulticast(ctx, msg)
	if err != nil {
		return nil, err
	}

	for i, r := range resp.Responses {
		if r.Success {
			continue
		}
		if messaging.IsUnregistered(r.Error) {
			invalid = append(invalid, tokens[i])
		} else {
			log.Printf("FCM send error for token #%d: %v", i, r.Error)
		}
	}
	if len(invalid) > 0 {
		log.Printf("FCM: %d token(s) reported invalid and will be pruned", len(invalid))
	}
	return invalid, nil
}
