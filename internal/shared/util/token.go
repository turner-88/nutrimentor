package util

import (
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
)

// NewResetToken returns a random URL-safe token and its SHA-256 hex hash.
// The raw token is emailed to the user; only the hash is stored.
func NewResetToken() (raw, hash string, err error) {
	b := make([]byte, 32)
	if _, err = rand.Read(b); err != nil {
		return "", "", err
	}
	raw = hex.EncodeToString(b)
	return raw, HashToken(raw), nil
}

// HashToken returns the SHA-256 hex digest of a raw token, for lookup.
func HashToken(raw string) string {
	sum := sha256.Sum256([]byte(raw))
	return hex.EncodeToString(sum[:])
}
