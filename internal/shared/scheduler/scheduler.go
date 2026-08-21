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

	"github.com/remorac/nutrimentor-app/internal/shared/config"
	"github.com/remorac/nutrimentor-app/internal/shared/notify"
)

// StartReminders launches all daily reminder goroutines. It is a no-op (logs
// once) when reminders are disabled. Each job is skipped individually when its
// configured time is malformed.
func StartReminders(ctx context.Context, cfg *config.Config, notifier *notify.Service) {
	if !cfg.ReminderEnabled {
		log.Println("Daily reminders disabled (REMINDER_ENABLED != true)")
		return
	}
	scheduleDaily(ctx, "daily-log", cfg.ReminderTime, notifier.DailyLogReminder)
	scheduleDaily(ctx, "article", cfg.ArticleReminderTime, notifier.ArticleReminder)
}

// scheduleDaily runs fn once a day at the given "HH:MM" local time until ctx is
// cancelled. A malformed time disables just this job.
func scheduleDaily(ctx context.Context, name, hm string, fn func(context.Context)) {
	hh, mm, ok := parseHM(hm)
	if !ok {
		log.Printf("Reminder %q disabled: invalid time %q (want HH:MM)", name, hm)
		return
	}
	log.Printf("Reminder %q scheduled for %02d:%02d local time", name, hh, mm)
	go func() {
		for {
			timer := time.NewTimer(untilNext(time.Now(), hh, mm))
			select {
			case <-ctx.Done():
				timer.Stop()
				return
			case <-timer.C:
				fn(ctx)
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
