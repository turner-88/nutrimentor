package store

import (
	"context"

	db "github.com/remorac/sebaya-app/internal/database/sqlc"
)

// --- user ---

func (s *Store) CreateUserTx(ctx context.Context, arg db.CreateUserParams, actorID int32) (int32, error) {
	var id int32
	err := s.execTx(ctx, func(q *db.Queries) error {
		res, err := q.CreateUser(ctx, arg)
		if err != nil {
			return err
		}
		lid, err := res.LastInsertId()
		if err != nil {
			return err
		}
		id = int32(lid)
		return s.auditLog(ctx, q, "user", id, "INSERT", arg, actorID)
	})
	return id, err
}

func (s *Store) UpdateUserProfileTx(ctx context.Context, arg db.UpdateUserProfileParams, actorID int32) error {
	return s.execTx(ctx, func(q *db.Queries) error {
		if err := q.UpdateUserProfile(ctx, arg); err != nil {
			return err
		}
		return s.auditLog(ctx, q, "user", arg.ID, "UPDATE", arg, actorID)
	})
}

func (s *Store) UpdateUserPasswordTx(ctx context.Context, arg db.UpdateUserPasswordParams, actorID int32) error {
	return s.execTx(ctx, func(q *db.Queries) error {
		if err := q.UpdateUserPassword(ctx, arg); err != nil {
			return err
		}
		return s.auditLog(ctx, q, "user", arg.ID, "UPDATE", map[string]any{"field": "password"}, actorID)
	})
}

func (s *Store) AssignUserGroupTx(ctx context.Context, arg db.AssignUserGroupParams, actorID int32) error {
	return s.execTx(ctx, func(q *db.Queries) error {
		if err := q.AssignUserGroup(ctx, arg); err != nil {
			return err
		}
		return s.auditLog(ctx, q, "user", arg.ID, "UPDATE", arg, actorID)
	})
}

func (s *Store) SetUserActiveTx(ctx context.Context, arg db.SetUserActiveParams, actorID int32) error {
	return s.execTx(ctx, func(q *db.Queries) error {
		if err := q.SetUserActive(ctx, arg); err != nil {
			return err
		}
		return s.auditLog(ctx, q, "user", arg.ID, "UPDATE", arg, actorID)
	})
}

// --- peer_group ---

func (s *Store) CreatePeerGroupTx(ctx context.Context, arg db.CreatePeerGroupParams, actorID int32) (int32, error) {
	var id int32
	err := s.execTx(ctx, func(q *db.Queries) error {
		res, err := q.CreatePeerGroup(ctx, arg)
		if err != nil {
			return err
		}
		lid, err := res.LastInsertId()
		if err != nil {
			return err
		}
		id = int32(lid)
		return s.auditLog(ctx, q, "peer_group", id, "INSERT", arg, actorID)
	})
	return id, err
}

func (s *Store) UpdatePeerGroupTx(ctx context.Context, arg db.UpdatePeerGroupParams, actorID int32) error {
	return s.execTx(ctx, func(q *db.Queries) error {
		if err := q.UpdatePeerGroup(ctx, arg); err != nil {
			return err
		}
		return s.auditLog(ctx, q, "peer_group", arg.ID, "UPDATE", arg, actorID)
	})
}

func (s *Store) DeletePeerGroupTx(ctx context.Context, id int32, actorID int32) error {
	return s.execTx(ctx, func(q *db.Queries) error {
		if err := q.DeletePeerGroup(ctx, id); err != nil {
			return err
		}
		return s.auditLog(ctx, q, "peer_group", id, "DELETE", map[string]any{"id": id}, actorID)
	})
}

// --- daily logs (patient self-service; actor = the patient) ---

func (s *Store) UpsertMedicationLogTx(ctx context.Context, arg db.UpsertMedicationLogParams) error {
	return s.execTx(ctx, func(q *db.Queries) error {
		if _, err := q.UpsertMedicationLog(ctx, arg); err != nil {
			return err
		}
		return s.auditLog(ctx, q, "medication_log", arg.UserID, "UPSERT", arg, arg.UserID)
	})
}

func (s *Store) UpsertActivityLogTx(ctx context.Context, arg db.UpsertActivityLogParams) error {
	return s.execTx(ctx, func(q *db.Queries) error {
		if _, err := q.UpsertActivityLog(ctx, arg); err != nil {
			return err
		}
		return s.auditLog(ctx, q, "activity_log", arg.UserID, "UPSERT", arg, arg.UserID)
	})
}

func (s *Store) UpsertDietLogTx(ctx context.Context, arg db.UpsertDietLogParams) error {
	return s.execTx(ctx, func(q *db.Queries) error {
		if _, err := q.UpsertDietLog(ctx, arg); err != nil {
			return err
		}
		return s.auditLog(ctx, q, "diet_log", arg.UserID, "UPSERT", arg, arg.UserID)
	})
}

func (s *Store) CreateGlucoseLogTx(ctx context.Context, arg db.CreateGlucoseLogParams) (int32, error) {
	var id int32
	err := s.execTx(ctx, func(q *db.Queries) error {
		res, err := q.CreateGlucoseLog(ctx, arg)
		if err != nil {
			return err
		}
		lid, err := res.LastInsertId()
		if err != nil {
			return err
		}
		id = int32(lid)
		return s.auditLog(ctx, q, "glucose_log", id, "INSERT", arg, arg.UserID)
	})
	return id, err
}

// --- education_article ---

func (s *Store) CreateArticleTx(ctx context.Context, arg db.CreateArticleParams, actorID int32) (int32, error) {
	var id int32
	err := s.execTx(ctx, func(q *db.Queries) error {
		res, err := q.CreateArticle(ctx, arg)
		if err != nil {
			return err
		}
		lid, err := res.LastInsertId()
		if err != nil {
			return err
		}
		id = int32(lid)
		return s.auditLog(ctx, q, "education_article", id, "INSERT", arg, actorID)
	})
	return id, err
}

func (s *Store) UpdateArticleTx(ctx context.Context, arg db.UpdateArticleParams, actorID int32) error {
	return s.execTx(ctx, func(q *db.Queries) error {
		if err := q.UpdateArticle(ctx, arg); err != nil {
			return err
		}
		return s.auditLog(ctx, q, "education_article", arg.ID, "UPDATE", arg, actorID)
	})
}

func (s *Store) DeleteArticleTx(ctx context.Context, id int32, actorID int32) error {
	return s.execTx(ctx, func(q *db.Queries) error {
		if err := q.DeleteArticle(ctx, id); err != nil {
			return err
		}
		return s.auditLog(ctx, q, "education_article", id, "DELETE", map[string]any{"id": id}, actorID)
	})
}
