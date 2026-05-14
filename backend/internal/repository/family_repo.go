package repository

import (
	"context"

	"github.com/garyshker/mektep-api/internal/models"
	"github.com/jmoiron/sqlx"
)

type FamilyRepository struct {
	db *sqlx.DB
}

func NewFamilyRepository(db *sqlx.DB) *FamilyRepository {
	return &FamilyRepository{db: db}
}

func (r *FamilyRepository) Create(ctx context.Context, family *models.Family) error {
	query := `INSERT INTO families (name) VALUES ($1) RETURNING id, created_at`
	return r.db.QueryRowContext(ctx, query, family.Name).Scan(&family.ID, &family.CreatedAt)
}

func (r *FamilyRepository) GetByID(ctx context.Context, id string) (*models.Family, error) {
	family := &models.Family{}
	err := r.db.GetContext(ctx, family, "SELECT * FROM families WHERE id = $1", id)
	if err != nil {
		return nil, err
	}
	return family, nil
}

func (r *FamilyRepository) AddMember(ctx context.Context, member *models.FamilyMember) error {
	query := `INSERT INTO family_members (family_id, user_id, role) VALUES ($1, $2, $3) RETURNING id`
	return r.db.QueryRowContext(ctx, query, member.FamilyID, member.UserID, member.Role).Scan(&member.ID)
}

func (r *FamilyRepository) ListMembers(ctx context.Context, familyID string) ([]models.FamilyMember, error) {
	var members []models.FamilyMember
	err := r.db.SelectContext(ctx, &members, "SELECT * FROM family_members WHERE family_id = $1", familyID)
	return members, err
}

func (r *FamilyRepository) CreateInviteCode(ctx context.Context, invite *models.InviteCode) error {
	query := `INSERT INTO invite_codes (family_id, code, expires_at) VALUES ($1, $2, $3) RETURNING id`
	return r.db.QueryRowContext(ctx, query, invite.FamilyID, invite.Code, invite.ExpiresAt).Scan(&invite.ID)
}

func (r *FamilyRepository) GetInviteByCode(ctx context.Context, code string) (*models.InviteCode, error) {
	invite := &models.InviteCode{}
	err := r.db.GetContext(ctx, invite,
		"SELECT * FROM invite_codes WHERE code = $1 AND used = FALSE AND expires_at > NOW()", code)
	if err != nil {
		return nil, err
	}
	return invite, nil
}

func (r *FamilyRepository) MarkInviteUsed(ctx context.Context, id string) error {
	_, err := r.db.ExecContext(ctx, "UPDATE invite_codes SET used = TRUE WHERE id = $1", id)
	return err
}

func (r *FamilyRepository) GetFamilyByUserID(ctx context.Context, userID string) (*models.Family, error) {
	family := &models.Family{}
	err := r.db.GetContext(ctx, family, `
		SELECT f.* FROM families f
		JOIN family_members fm ON fm.family_id = f.id
		WHERE fm.user_id = $1 LIMIT 1`, userID)
	if err != nil {
		return nil, err
	}
	return family, nil
}
