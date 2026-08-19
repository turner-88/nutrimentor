package util

import (
	"fmt"

	"golang.org/x/crypto/bcrypt"
)

// HashPassword returns the bcrypt hash of a plaintext password.
func HashPassword(plain string) (string, error) {
	b, err := bcrypt.GenerateFromPassword([]byte(plain), bcrypt.DefaultCost)
	return string(b), err
}

// CheckPassword reports whether plain matches the stored bcrypt hash.
func CheckPassword(hash, plain string) bool {
	return bcrypt.CompareHashAndPassword([]byte(hash), []byte(plain)) == nil
}

// ValidatePassword enforces a minimal length policy (Indonesian message).
func ValidatePassword(password string) error {
	if len(password) < 6 {
		return fmt.Errorf("Password minimal 6 karakter.")
	}
	if len(password) > 72 {
		return fmt.Errorf("Password maksimal 72 karakter.")
	}
	return nil
}
