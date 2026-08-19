package handler

import (
	"database/sql"
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
