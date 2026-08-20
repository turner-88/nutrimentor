package handler

import (
	"database/sql"
	"fmt"
	"regexp"
	"strings"
)

func toNullInt32(v int32) sql.NullInt32 {
	return sql.NullInt32{Int32: v, Valid: v > 0}
}

var slugRe = regexp.MustCompile(`[^a-z0-9]+`)

// slugify turns a title into a URL-safe slug.
func slugify(s string) string {
	s = strings.ToLower(strings.TrimSpace(s))
	s = slugRe.ReplaceAllString(s, "-")
	return strings.Trim(s, "-")
}

// uniqueSlug returns base, or base-2 / base-3 / … if base is already taken.
func uniqueSlug(base string, taken map[string]bool) string {
	if base == "" {
		base = "artikel"
	}
	s := base
	for i := 2; taken[s]; i++ {
		s = fmt.Sprintf("%s-%d", base, i)
	}
	return s
}
