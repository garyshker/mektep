package services

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/garyshker/mektep-api/internal/models"
	"github.com/garyshker/mektep-api/internal/repository"
)

type LessonService struct {
	lessonRepo  *repository.LessonRepository
	subjectRepo *repository.SubjectRepository
}

func NewLessonService(lessonRepo *repository.LessonRepository, subjectRepo *repository.SubjectRepository) *LessonService {
	return &LessonService{lessonRepo: lessonRepo, subjectRepo: subjectRepo}
}

func (s *LessonService) ListSubjects(ctx context.Context) ([]models.Subject, error) {
	return s.subjectRepo.List(ctx)
}

func (s *LessonService) ListLessons(ctx context.Context, subjectID string, gradeLevel int) ([]models.Lesson, error) {
	return s.lessonRepo.ListBySubject(ctx, subjectID, gradeLevel)
}

func (s *LessonService) GetLesson(ctx context.Context, lessonID string) (*models.Lesson, error) {
	return s.lessonRepo.GetWithQuestions(ctx, lessonID)
}

// LessonUpload is the JSON format for uploading lessons via CLI or API
type LessonUpload struct {
	Subject        string         `json:"subject"`
	GradeLevel     int            `json:"grade_level"`
	DifficultyTier int            `json:"difficulty_tier"`
	SortOrder      int            `json:"sort_order"`
	Title          models.I18nText `json:"title"`
	Description    models.I18nText `json:"description"`
	Questions      []QuestionUpload `json:"questions"`
}

type QuestionUpload struct {
	Type             models.QuestionType `json:"type"`
	Prompt           models.I18nText     `json:"prompt"`
	MediaURL         string              `json:"media_url,omitempty"`
	Options          json.RawMessage     `json:"options,omitempty"`
	CorrectAnswer    json.RawMessage     `json:"correct_answer"`
	Pairs            json.RawMessage     `json:"pairs,omitempty"`
	DifficultyScore  int                 `json:"difficulty_score"`
	TimeLimitSeconds int                 `json:"time_limit_seconds,omitempty"`
}

func (s *LessonService) ValidateUpload(upload *LessonUpload) []string {
	var errs []string

	if upload.Subject == "" {
		errs = append(errs, "subject is required")
	}
	if upload.GradeLevel < 1 || upload.GradeLevel > 12 {
		errs = append(errs, "grade_level must be between 1 and 12")
	}
	if len(upload.Title) == 0 {
		errs = append(errs, "title is required (at least one language)")
	}
	if len(upload.Questions) == 0 {
		errs = append(errs, "at least one question is required")
	}

	validTypes := map[models.QuestionType]bool{
		models.QuestionMC: true, models.QuestionType_: true,
		models.QuestionTap: true, models.QuestionWord: true,
		models.QuestionMatch: true, models.QuestionDrag: true,
		models.QuestionAudio: true,
	}

	for i, q := range upload.Questions {
		if !validTypes[q.Type] {
			errs = append(errs, fmt.Sprintf("question[%d]: invalid type %q", i, q.Type))
		}
		if len(q.Prompt) == 0 {
			errs = append(errs, fmt.Sprintf("question[%d]: prompt is required", i))
		}
		if q.CorrectAnswer == nil && q.Type != models.QuestionMatch {
			errs = append(errs, fmt.Sprintf("question[%d]: correct_answer is required", i))
		}
	}

	return errs
}

func (s *LessonService) UploadLesson(ctx context.Context, upload *LessonUpload) (*models.Lesson, error) {
	if errs := s.ValidateUpload(upload); len(errs) > 0 {
		return nil, fmt.Errorf("validation errors: %v", errs)
	}

	// Find or resolve subject ID by name
	subjects, err := s.subjectRepo.List(ctx)
	if err != nil {
		return nil, err
	}

	var subjectID string
	for _, sub := range subjects {
		if sub.Name["en"] == upload.Subject || sub.ID == upload.Subject {
			subjectID = sub.ID
			break
		}
	}
	if subjectID == "" {
		return nil, fmt.Errorf("subject %q not found", upload.Subject)
	}

	lesson := &models.Lesson{
		SubjectID:      subjectID,
		Title:          upload.Title,
		Description:    upload.Description,
		GradeLevel:     upload.GradeLevel,
		DifficultyTier: upload.DifficultyTier,
		SortOrder:      upload.SortOrder,
	}

	if err := s.lessonRepo.CreateLesson(ctx, lesson); err != nil {
		return nil, fmt.Errorf("creating lesson: %w", err)
	}

	for i, qu := range upload.Questions {
		q := &models.Question{
			LessonID:         lesson.ID,
			Type:             qu.Type,
			Prompt:           qu.Prompt,
			MediaURL:         qu.MediaURL,
			Options:          qu.Options,
			CorrectAnswer:    qu.CorrectAnswer,
			Pairs:            qu.Pairs,
			DifficultyScore:  qu.DifficultyScore,
			TimeLimitSeconds: qu.TimeLimitSeconds,
			SortOrder:        i + 1,
		}
		if err := s.lessonRepo.CreateQuestion(ctx, q); err != nil {
			return nil, fmt.Errorf("creating question %d: %w", i+1, err)
		}
	}

	return s.lessonRepo.GetWithQuestions(ctx, lesson.ID)
}

func (s *LessonService) DeleteLesson(ctx context.Context, lessonID string) error {
	return s.lessonRepo.DeleteLesson(ctx, lessonID)
}

func (s *LessonService) ExportLessons(ctx context.Context, subjectID string, gradeLevel int) ([]models.Lesson, error) {
	return s.lessonRepo.ExportBySubject(ctx, subjectID, gradeLevel)
}
