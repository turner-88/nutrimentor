// Package scheduler runs the once-a-day log-reminder push notification.
//
// It is a single in-process job (no external cron dependency): a goroutine
// sleeps until the next configured local time, fires, and repeats. It stops
// when the provided context is cancelled (graceful shutdown).
package scheduler

import (
	"context"
	"log"
	"time"

	"github.com/remorac/sebaya-app/internal/shared/config"
	"github.com/remorac/sebaya-app/internal/shared/notify"
)

// StartDailyReminder launches the reminder goroutine. It is a no-op (logs once)
// when reminders are disabled or REMINDER_TIME is malformed.
func StartDailyReminder(ctx context.Context, cfg *config.Config, notifier *notify.Service) {
	if !cfg.ReminderEnabled {
		log.Println("Daily reminder disabled (REMINDER_ENABLED != true)")
		return
	}
	hh, mm, ok := parseHM(cfg.ReminderTime)
	if !ok {
		log.Printf("Daily reminder disabled: invalid REMINDER_TIME %q (want HH:MM)", cfg.ReminderTime)
		return
	}

	log.Printf("Daily reminder scheduled for %02d:%02d local time", hh, mm)
	go func() {
		for {
			d := untilNext(time.Now(), hh, mm)
			timer := time.NewTimer(d)
			select {
			case <-ctx.Done():
				timer.Stop()
				return
			case <-timer.C:
				notifier.DailyReminder(ctx)
			}
		}
	}()
}

// untilNext returns the duration from now until the next occurrence of hh:mm.
func untilNext(now time.Time, hh, mm int) time.Duration {
	next := time.Date(now.Year(), now.Month(), now.Day(), hh, mm, 0, 0, now.Location())
	if !next.After(now) {
		next = next.Add(24 * time.Hour)
	}
	return next.Sub(now)
}

// parseHM parses a "HH:MM" 24-hour string.
func parseHM(s string) (hh, mm int, ok bool) {
	var h, m int
	if n, err := time.Parse("15:04", s); err == nil {
		h, m = n.Hour(), n.Minute()
		return h, m, true
	}
	return 0, 0, false
}
