package repository

import (
	"context"
	"encoding/json"

	"github.com/garyshker/mektep-api/internal/models"
	"github.com/jmoiron/sqlx"
)

type LessonRepository struct {
	db *sqlx.DB
}

func NewLessonRepository(db *sqlx.DB) *LessonRepository {
	return &LessonRepository{db: db}
}

func (r *LessonRepository) ListBySubject(ctx context.Context, subjectID string, gradeLevel int) ([]models.Lesson, error) {
	var lessons []models.Lesson
	query := "SELECT * FROM lessons WHERE subject_id = $1"
	args := []interface{}{subjectID}

	if gradeLevel > 0 {
		query += " AND grade_level = $2"
		args = append(args, gradeLevel)
	}
	query += " ORDER BY sort_order"

	err := r.db.SelectContext(ctx, &lessons, query, args...)
	return lessons, err
}

func (r *LessonRepository) GetByID(ctx context.Context, id string) (*models.Lesson, error) {
	lesson := &models.Lesson{}
	err := r.db.GetContext(ctx, lesson, "SELECT * FROM lessons WHERE id = $1", id)
	if err != nil {
		return nil, err
	}
	return lesson, nil
}

func (r *LessonRepository) GetWithQuestions(ctx context.Context, id string) (*models.Lesson, error) {
	lesson, err := r.GetByID(ctx, id)
	if err != nil {
		return nil, err
	}

	var questions []models.Question
	err = r.db.SelectContext(ctx, &questions,
		"SELECT * FROM questions WHERE lesson_id = $1 ORDER BY sort_order", id)
	if err != nil {
		return nil, err
	}
	lesson.Questions = questions
	return lesson, nil
}

func (r *LessonRepository) CreateLesson(ctx context.Context, lesson *models.Lesson) error {
	titleJSON, _ := json.Marshal(lesson.Title)
	descJSON, _ := json.Marshal(lesson.Description)

	query := `
		INSERT INTO lessons (subject_id, title, description, grade_level, difficulty_tier, sort_order)
		VALUES ($1, $2, $3, $4, $5, $6)
		RETURNING id`
	return r.db.QueryRowContext(ctx, query,
		lesson.SubjectID, titleJSON, descJSON,
		lesson.GradeLevel, lesson.DifficultyTier, lesson.SortOrder,
	).Scan(&lesson.ID)
}

func (r *LessonRepository) CreateQuestion(ctx context.Context, q *models.Question) error {
	promptJSON, _ := json.Marshal(q.Prompt)

	query := `
		INSERT INTO questions (lesson_id, type, prompt, media_url, options, correct_answer, pairs, difficulty_score, time_limit_seconds, sort_order)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
		RETURNING id`
	return r.db.QueryRowContext(ctx, query,
		q.LessonID, q.Type, promptJSON, q.MediaURL,
		q.Options, q.CorrectAnswer, q.Pairs,
		q.DifficultyScore, q.TimeLimitSeconds, q.SortOrder,
	).Scan(&q.ID)
}

func (r *LessonRepository) DeleteLesson(ctx context.Context, id string) error {
	_, err := r.db.ExecContext(ctx, "DELETE FROM lessons WHERE id = $1", id)
	return err
}

func (r *LessonRepository) ExportBySubject(ctx context.Context, subjectID string, gradeLevel int) ([]models.Lesson, error) {
	lessons, err := r.ListBySubject(ctx, subjectID, gradeLevel)
	if err != nil {
		return nil, err
	}

	for i := range lessons {
		var questions []models.Question
		err := r.db.SelectContext(ctx, &questions,
			"SELECT * FROM questions WHERE lesson_id = $1 ORDER BY sort_order", lessons[i].ID)
		if err != nil {
			return nil, err
		}
		lessons[i].Questions = questions
	}

	return lessons, nil
}
