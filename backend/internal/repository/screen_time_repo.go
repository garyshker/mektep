package repository

import (
	"context"

	"github.com/garyshker/mektep-api/internal/models"
	"github.com/jmoiron/sqlx"
)

type ScreenTimeRepository struct {
	db *sqlx.DB
}

func NewScreenTimeRepository(db *sqlx.DB) *ScreenTimeRepository {
	return &ScreenTimeRepository{db: db}
}

func (r *ScreenTimeRepository) GetConfig(ctx context.Context, childID string) (*models.ScreenTimeConfig, error) {
	cfg := &models.ScreenTimeConfig{}
	err := r.db.GetContext(ctx, cfg, "SELECT * FROM screen_time_config WHERE child_id = $1", childID)
	if err != nil {
		return nil, err
	}
	return cfg, nil
}

func (r *ScreenTimeRepository) UpsertConfig(ctx context.Context, cfg *models.ScreenTimeConfig) error {
	query := `
		INSERT INTO screen_time_config (family_id, child_id, points_per_minute, daily_max_minutes, bedtime_start, bedtime_end, blocked_apps, allowed_apps)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
		ON CONFLICT (child_id) DO UPDATE SET
			points_per_minute = EXCLUDED.points_per_minute,
			daily_max_minutes = EXCLUDED.daily_max_minutes,
			bedtime_start = EXCLUDED.bedtime_start,
			bedtime_end = EXCLUDED.bedtime_end,
			blocked_apps = EXCLUDED.blocked_apps,
			allowed_apps = EXCLUDED.allowed_apps
		RETURNING id`
	return r.db.QueryRowContext(ctx, query,
		cfg.FamilyID, cfg.ChildID, cfg.PointsPerMinute, cfg.DailyMaxMinutes,
		cfg.BedtimeStart, cfg.BedtimeEnd, cfg.BlockedApps, cfg.AllowedApps,
	).Scan(&cfg.ID)
}

func (r *ScreenTimeRepository) AddTransaction(ctx context.Context, txn *models.ScreenTimeTransaction) error {
	query := `
		INSERT INTO screen_time_transactions (child_id, type, amount_seconds, source)
		VALUES ($1, $2, $3, $4)
		RETURNING id, created_at`
	return r.db.QueryRowContext(ctx, query,
		txn.ChildID, txn.Type, txn.AmountSeconds, txn.Source,
	).Scan(&txn.ID, &txn.CreatedAt)
}

func (r *ScreenTimeRepository) GetTransactions(ctx context.Context, childID string, limit int) ([]models.ScreenTimeTransaction, error) {
	var txns []models.ScreenTimeTransaction
	err := r.db.SelectContext(ctx, &txns,
		"SELECT * FROM screen_time_transactions WHERE child_id = $1 ORDER BY created_at DESC LIMIT $2",
		childID, limit)
	return txns, err
}
