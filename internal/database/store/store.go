package store

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"reflect"
	"strings"
	"time"

	db "github.com/remorac/sebaya-app/internal/database/sqlc"
)

// Store wraps sqlc.Queries and adds transactional mutations that also write an
// audit_trail row atomically. Handlers call the *Tx methods for writes.
type Store struct {
	*db.Queries
	conn *sql.DB
}

func New(conn *sql.DB) *Store {
	return &Store{Queries: db.New(conn), conn: conn}
}

// execTx runs fn inside a single transaction, rolling back on error.
func (s *Store) execTx(ctx context.Context, fn func(*db.Queries) error) error {
	tx, err := s.conn.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	q := db.New(tx)
	if err = fn(q); err != nil {
		if rbErr := tx.Rollback(); rbErr != nil {
			return fmt.Errorf("tx err: %v, rb err: %v", err, rbErr)
		}
		return err
	}
	return tx.Commit()
}

// auditLog inserts a row into audit_trail within the current transaction.
func (s *Store) auditLog(ctx context.Context, q *db.Queries, table string, rowID int32, action string, changes any, actorID int32) error {
	payload, err := json.Marshal(flattenForAudit(changes))
	if err != nil {
		return err
	}
	_, err = q.CreateAuditTrail(ctx, db.CreateAuditTrailParams{
		TableName: table,
		RowID:     rowID,
		Action:    action,
		Changes:   sql.NullString{String: string(payload), Valid: true},
		ActorID:   sql.NullInt32{Int32: actorID, Valid: actorID > 0},
	})
	return err
}

// flattenForAudit unwraps sql.Null* fields of a struct into a plain map for
// readable audit JSON. Maps and non-structs pass through unchanged.
func flattenForAudit(v any) any {
	if v == nil {
		return nil
	}
	rv := reflect.ValueOf(v)
	if rv.Kind() == reflect.Map {
		return v
	}
	if rv.Kind() == reflect.Ptr {
		rv = rv.Elem()
	}
	if rv.Kind() != reflect.Struct {
		return v
	}
	rt := rv.Type()
	out := make(map[string]any, rt.NumField())
	for i := 0; i < rt.NumField(); i++ {
		key := rt.Field(i).Tag.Get("json")
		if key == "" || key == "-" {
			continue
		}
		if idx := strings.IndexByte(key, ','); idx >= 0 {
			key = key[:idx]
		}
		switch val := rv.Field(i).Interface().(type) {
		case sql.NullString:
			out[key] = nullable(val.Valid, val.String)
		case sql.NullInt32:
			out[key] = nullable(val.Valid, val.Int32)
		case sql.NullInt64:
			out[key] = nullable(val.Valid, val.Int64)
		case sql.NullBool:
			out[key] = nullable(val.Valid, val.Bool)
		case sql.NullTime:
			if val.Valid {
				out[key] = val.Time.Format(time.RFC3339)
			} else {
				out[key] = nil
			}
		default:
			out[key] = val
		}
	}
	return out
}

func nullable[T any](valid bool, v T) any {
	if valid {
		return v
	}
	return nil
}
