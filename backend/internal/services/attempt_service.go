package services

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"
	"time"

	"github.com/garyshker/mektep-api/internal/models"
	"github.com/garyshker/mektep-api/internal/repository"
)

type AttemptService struct {
	attemptRepo *repository.AttemptRepository
	lessonRepo  *repository.LessonRepository
	userRepo    *repository.UserRepository
}

func NewAttemptService(attemptRepo *repository.AttemptRepository, lessonRepo *repository.LessonRepository, userRepo *repository.UserRepository) *AttemptService {
	return &AttemptService{attemptRepo: attemptRepo, lessonRepo: lessonRepo, userRepo: userRepo}
}

func (s *AttemptService) Start(ctx context.Context, childID, lessonID string) (*models.LessonAttempt, error) {
	// Verify lesson exists
	_, err := s.lessonRepo.GetByID(ctx, lessonID)
	if err != nil {
		return nil, fmt.Errorf("lesson not found: %w", err)
	}

	attempt := &models.LessonAttempt{
		ChildID:  childID,
		LessonID: lessonID,
		Status:   models.AttemptInProgress,
	}

	if err := s.attemptRepo.Create(ctx, attempt); err != nil {
		return nil, err
	}

	return attempt, nil
}

type SubmitAnswerRequest struct {
	QuestionID  string `json:"question_id"`
	GivenAnswer string `json:"given_answer"`
	TimeSpentMs int    `json:"time_spent_ms"`
}

type SubmitAnswerResponse struct {
	IsCorrect     bool   `json:"is_correct"`
	CorrectAnswer string `json:"correct_answer,omitempty"`
}

func (s *AttemptService) SubmitAnswer(ctx context.Context, attemptID string, req SubmitAnswerRequest) (*SubmitAnswerResponse, error) {
	attempt, err := s.attemptRepo.GetByID(ctx, attemptID)
	if err != nil {
		return nil, fmt.Errorf("attempt not found: %w", err)
	}

	if attempt.Status != models.AttemptInProgress {
		return nil, fmt.Errorf("attempt is not in progress")
	}

	// Get the lesson with questions to check the answer
	lesson, err := s.lessonRepo.GetWithQuestions(ctx, attempt.LessonID)
	if err != nil {
		return nil, err
	}

	var question *models.Question
	for i := range lesson.Questions {
		if lesson.Questions[i].ID == req.QuestionID {
			question = &lesson.Questions[i]
			break
		}
	}
	if question == nil {
		return nil, fmt.Errorf("question not found in lesson")
	}

	// Check correctness
	isCorrect := checkAnswer(question, req.GivenAnswer)

	answer := &models.AttemptAnswer{
		AttemptID:   attemptID,
		QuestionID:  req.QuestionID,
		GivenAnswer: req.GivenAnswer,
		IsCorrect:   isCorrect,
		TimeSpentMs: req.TimeSpentMs,
	}

	if err := s.attemptRepo.SaveAnswer(ctx, answer); err != nil {
		return nil, err
	}

	resp := &SubmitAnswerResponse{IsCorrect: isCorrect}
	if !isCorrect {
		resp.CorrectAnswer = string(question.CorrectAnswer)
	}

	return resp, nil
}

func checkAnswer(q *models.Question, given string) bool {
	var correct string
	if err := json.Unmarshal(q.CorrectAnswer, &correct); err != nil {
		// Try as number
		var correctNum json.Number
		if err := json.Unmarshal(q.CorrectAnswer, &correctNum); err == nil {
			correct = correctNum.String()
		} else {
			// Try as index
			var correctIdx int
			if err := json.Unmarshal(q.CorrectAnswer, &correctIdx); err == nil {
				correct = fmt.Sprintf("%d", correctIdx)
			}
		}
	}

	return strings.EqualFold(strings.TrimSpace(given), strings.TrimSpace(correct))
}

type CompleteResponse struct {
	Score       int     `json:"score"`
	AccuracyPct float64 `json:"accuracy_pct"`
	StarsEarned int     `json:"stars_earned"`
	XPEarned    int     `json:"xp_earned"`
}

func (s *AttemptService) Complete(ctx context.Context, attemptID string) (*CompleteResponse, error) {
	attempt, err := s.attemptRepo.GetByID(ctx, attemptID)
	if err != nil {
		return nil, err
	}

	answers, err := s.attemptRepo.GetAnswersByAttempt(ctx, attemptID)
	if err != nil {
		return nil, err
	}

	totalQuestions := len(answers)
	if totalQuestions == 0 {
		return nil, fmt.Errorf("no answers submitted")
	}

	correct := 0
	for _, a := range answers {
		if a.IsCorrect {
			correct++
		}
	}

	accuracy := float64(correct) / float64(totalQuestions) * 100

	// Star rating
	stars := 1
	if accuracy >= 80 {
		stars = 2
	}
	if accuracy >= 95 {
		stars = 3
	}

	// XP: 5 per correct answer + bonus for stars
	xp := correct * 5
	if stars == 2 {
		xp += 10
	} else if stars == 3 {
		xp += 20
	}

	attempt.Score = correct
	attempt.AccuracyPct = accuracy
	attempt.StarsEarned = stars
	attempt.XPEarned = xp

	if err := s.attemptRepo.Complete(ctx, attempt); err != nil {
		return nil, err
	}

	// Update child XP
	if err := s.userRepo.UpdateChildXP(ctx, attempt.ChildID, xp); err != nil {
		return nil, err
	}

	// Update streak
	today := time.Now().Format("2006-01-02")
	profile, err := s.userRepo.GetChildProfile(ctx, attempt.ChildID)
	if err == nil {
		newStreak := 1
		if profile.LastActiveDate == today {
			newStreak = profile.CurrentStreak // already counted today
		} else if profile.LastActiveDate == time.Now().AddDate(0, 0, -1).Format("2006-01-02") {
			newStreak = profile.CurrentStreak + 1
		}
		s.userRepo.UpdateStreak(ctx, attempt.ChildID, newStreak, today)
	}

	return &CompleteResponse{
		Score:       correct,
		AccuracyPct: accuracy,
		StarsEarned: stars,
		XPEarned:    xp,
	}, nil
}

type DashboardResponse struct {
	Profile  *models.ChildProfile            `json:"profile"`
	Progress []repository.SubjectProgress    `json:"progress"`
}

func (s *AttemptService) Dashboard(ctx context.Context, childID string) (*DashboardResponse, error) {
	profile, err := s.userRepo.GetChildProfile(ctx, childID)
	if err != nil {
		return nil, err
	}

	progress, err := s.attemptRepo.GetChildSubjectProgress(ctx, childID)
	if err != nil {
		return nil, err
	}

	return &DashboardResponse{
		Profile:  profile,
		Progress: progress,
	}, nil
}

func (s *AttemptService) Progress(ctx context.Context, childID string) ([]repository.SubjectProgress, error) {
	return s.attemptRepo.GetChildSubjectProgress(ctx, childID)
}
