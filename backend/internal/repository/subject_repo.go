package repository

import (
	"context"

	"github.com/garyshker/mektep-api/internal/models"
	"github.com/jmoiron/sqlx"
)

type SubjectRepository struct {
	db *sqlx.DB
}

func NewSubjectRepository(db *sqlx.DB) *SubjectRepository {
	return &SubjectRepository{db: db}
}

func (r *SubjectRepository) List(ctx context.Context) ([]models.Subject, error) {
	var subjects []models.Subject
	err := r.db.SelectContext(ctx, &subjects, "SELECT * FROM subjects WHERE is_active = TRUE ORDER BY name")
	return subjects, err
}

func (r *SubjectRepository) GetByID(ctx context.Context, id string) (*models.Subject, error) {
	subject := &models.Subject{}
	err := r.db.GetContext(ctx, subject, "SELECT * FROM subjects WHERE id = $1", id)
	if err != nil {
		return nil, err
	}
	return subject, nil
}

func (r *SubjectRepository) Create(ctx context.Context, subject *models.Subject) error {
	query := `INSERT INTO subjects (name, icon, color_scheme, is_active) VALUES ($1, $2, $3, $4) RETURNING id`
	return r.db.QueryRowContext(ctx, query, subject.Name, subject.Icon, subject.ColorScheme, subject.IsActive).Scan(&subject.ID)
}
