package model

// UserRole mirrors the user.role ENUM in the database.
type UserRole string

const (
	UserRoleAdmin   UserRole = "admin"
	UserRolePatient UserRole = "patient"
)

// User is placed in the request context by the auth middleware after JWT
// validation. It carries only what authorization checks need — no secrets.
type User struct {
	ID       int32    `json:"id"`
	Role     UserRole `json:"role"`
	Username string   `json:"username,omitempty"`
}

func (u *User) IsAdmin() bool   { return u.Role == UserRoleAdmin }
func (u *User) IsPatient() bool { return u.Role == UserRolePatient }
