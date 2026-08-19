package util

import (
	"fmt"
	"log"
	"net/smtp"
	"strings"

	"github.com/remorac/sebaya-app/internal/shared/config"
)

// SendMail sends a plain-text email via SMTP. When no SMTP host is configured
// (typical in development) it logs the message instead of sending, so flows
// that depend on email remain testable without a real mail server.
func SendMail(cfg config.SMTPConfig, to, subject, body string) error {
	if cfg.Host == "" {
		log.Printf("[mail] SMTP not configured — would send to %s\nSubject: %s\n%s", to, subject, body)
		return nil
	}

	msg := strings.Join([]string{
		"From: " + cfg.From,
		"To: " + to,
		"Subject: " + subject,
		"MIME-Version: 1.0",
		"Content-Type: text/plain; charset=UTF-8",
		"",
		body,
	}, "\r\n")

	addr := cfg.Host + ":" + cfg.Port
	var auth smtp.Auth
	if cfg.Username != "" {
		auth = smtp.PlainAuth("", cfg.Username, cfg.Password, cfg.Host)
	}
	if err := smtp.SendMail(addr, auth, cfg.From, []string{to}, []byte(msg)); err != nil {
		return fmt.Errorf("send mail: %w", err)
	}
	return nil
}
