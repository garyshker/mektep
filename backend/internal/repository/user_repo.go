package repository

import (
	"context"

	"github.com/garyshker/mektep-api/internal/models"
	"github.com/jmoiron/sqlx"
)

type UserRepository struct {
	db *sqlx.DB
}

func NewUserRepository(db *sqlx.DB) *UserRepository {
	return &UserRepository{db: db}
}

func (r *UserRepository) Create(ctx context.Context, user *models.User) error {
	query := `
		INSERT INTO users (email, password_hash, role, language_preference, timezone)
		VALUES ($1, $2, $3, $4, $5)
		RETURNING id, created_at, updated_at`
	return r.db.QueryRowContext(ctx, query,
		user.Email, user.PasswordHash, user.Role, user.LanguagePreference, user.Timezone,
	).Scan(&user.ID, &user.CreatedAt, &user.UpdatedAt)
}

func (r *UserRepository) GetByEmail(ctx context.Context, email string) (*models.User, error) {
	user := &models.User{}
	err := r.db.GetContext(ctx, user, "SELECT * FROM users WHERE email = $1", email)
	if err != nil {
		return nil, err
	}
	return user, nil
}

func (r *UserRepository) GetByID(ctx context.Context, id string) (*models.User, error) {
	user := &models.User{}
	err := r.db.GetContext(ctx, user, "SELECT * FROM users WHERE id = $1", id)
	if err != nil {
		return nil, err
	}
	return user, nil
}

func (r *UserRepository) CreateChildProfile(ctx context.Context, profile *models.ChildProfile) error {
	query := `
		INSERT INTO child_profiles (user_id, display_name, avatar, date_of_birth, grade_level)
		VALUES ($1, $2, $3, $4, $5)
		RETURNING id, created_at`
	return r.db.QueryRowContext(ctx, query,
		profile.UserID, profile.DisplayName, profile.Avatar, profile.DateOfBirth, profile.GradeLevel,
	).Scan(&profile.ID, &profile.CreatedAt)
}

func (r *UserRepository) GetChildProfile(ctx context.Context, childID string) (*models.ChildProfile, error) {
	profile := &models.ChildProfile{}
	err := r.db.GetContext(ctx, profile, "SELECT * FROM child_profiles WHERE id = $1", childID)
	if err != nil {
		return nil, err
	}
	return profile, nil
}

func (r *UserRepository) GetChildProfileByUserID(ctx context.Context, userID string) (*models.ChildProfile, error) {
	profile := &models.ChildProfile{}
	err := r.db.GetContext(ctx, profile, "SELECT * FROM child_profiles WHERE user_id = $1", userID)
	if err != nil {
		return nil, err
	}
	return profile, nil
}

func (r *UserRepository) UpdateChildXP(ctx context.Context, childID string, xpDelta int) error {
	_, err := r.db.ExecContext(ctx,
		"UPDATE child_profiles SET xp_total = xp_total + $1 WHERE id = $2",
		xpDelta, childID)
	return err
}

func (r *UserRepository) UpdateStreak(ctx context.Context, childID string, streak int, lastActive string) error {
	_, err := r.db.ExecContext(ctx, `
		UPDATE child_profiles
		SET current_streak = $1,
		    longest_streak = GREATEST(longest_streak, $1),
		    last_active_date = $2
		WHERE id = $3`,
		streak, lastActive, childID)
	return err
}

func (r *UserRepository) UpdateScreenTimeBalance(ctx context.Context, childID string, deltaSecs int) error {
	_, err := r.db.ExecContext(ctx,
		"UPDATE child_profiles SET screen_time_balance_seconds = screen_time_balance_seconds + $1 WHERE id = $2",
		deltaSecs, childID)
	return err
}
