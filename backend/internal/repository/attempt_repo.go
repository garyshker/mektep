package repository

import (
	"context"

	"github.com/garyshker/mektep-api/internal/models"
	"github.com/jmoiron/sqlx"
)

type AttemptRepository struct {
	db *sqlx.DB
}

func NewAttemptRepository(db *sqlx.DB) *AttemptRepository {
	return &AttemptRepository{db: db}
}

func (r *AttemptRepository) Create(ctx context.Context, attempt *models.LessonAttempt) error {
	query := `
		INSERT INTO lesson_attempts (child_id, lesson_id, status)
		VALUES ($1, $2, $3)
		RETURNING id, started_at`
	return r.db.QueryRowContext(ctx, query,
		attempt.ChildID, attempt.LessonID, attempt.Status,
	).Scan(&attempt.ID, &attempt.StartedAt)
}

func (r *AttemptRepository) GetByID(ctx context.Context, id string) (*models.LessonAttempt, error) {
	attempt := &models.LessonAttempt{}
	err := r.db.GetContext(ctx, attempt, "SELECT * FROM lesson_attempts WHERE id = $1", id)
	if err != nil {
		return nil, err
	}
	return attempt, nil
}

func (r *AttemptRepository) SaveAnswer(ctx context.Context, answer *models.AttemptAnswer) error {
	query := `
		INSERT INTO attempt_answers (attempt_id, question_id, given_answer, is_correct, time_spent_ms)
		VALUES ($1, $2, $3, $4, $5)
		RETURNING id`
	return r.db.QueryRowContext(ctx, query,
		answer.AttemptID, answer.QuestionID, answer.GivenAnswer, answer.IsCorrect, answer.TimeSpentMs,
	).Scan(&answer.ID)
}

func (r *AttemptRepository) Complete(ctx context.Context, attempt *models.LessonAttempt) error {
	_, err := r.db.ExecContext(ctx, `
		UPDATE lesson_attempts
		SET status = $1, completed_at = NOW(), score = $2, accuracy_pct = $3, stars_earned = $4, xp_earned = $5
		WHERE id = $6`,
		models.AttemptCompleted, attempt.Score, attempt.AccuracyPct, attempt.StarsEarned, attempt.XPEarned, attempt.ID)
	return err
}

func (r *AttemptRepository) GetAnswersByAttempt(ctx context.Context, attemptID string) ([]models.AttemptAnswer, error) {
	var answers []models.AttemptAnswer
	err := r.db.SelectContext(ctx, &answers, "SELECT * FROM attempt_answers WHERE attempt_id = $1", attemptID)
	return answers, err
}

func (r *AttemptRepository) GetChildAttempts(ctx context.Context, childID string) ([]models.LessonAttempt, error) {
	var attempts []models.LessonAttempt
	err := r.db.SelectContext(ctx, &attempts,
		"SELECT * FROM lesson_attempts WHERE child_id = $1 ORDER BY started_at DESC", childID)
	return attempts, err
}

func (r *AttemptRepository) GetChildSubjectProgress(ctx context.Context, childID string) ([]SubjectProgress, error) {
	var progress []SubjectProgress
	err := r.db.SelectContext(ctx, &progress, `
		SELECT
			l.subject_id,
			COUNT(DISTINCT la.lesson_id) FILTER (WHERE la.status = 'COMPLETED') as completed_lessons,
			COUNT(DISTINCT l.id) as total_lessons,
			COALESCE(MAX(la.stars_earned), 0) as best_stars,
			COALESCE(SUM(la.xp_earned), 0) as total_xp
		FROM lessons l
		LEFT JOIN lesson_attempts la ON la.lesson_id = l.id AND la.child_id = $1
		GROUP BY l.subject_id`, childID)
	return progress, err
}

type SubjectProgress struct {
	SubjectID        string `json:"subject_id" db:"subject_id"`
	CompletedLessons int    `json:"completed_lessons" db:"completed_lessons"`
	TotalLessons     int    `json:"total_lessons" db:"total_lessons"`
	BestStars        int    `json:"best_stars" db:"best_stars"`
	TotalXP          int    `json:"total_xp" db:"total_xp"`
}
