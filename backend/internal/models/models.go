package models

import (
	"encoding/json"
	"time"
)

// I18nText stores translatable text as {"kk": "...", "ru": "...", "en": "..."}
type I18nText map[string]string

// Role represents user roles
type Role string

const (
	RoleParent Role = "PARENT"
	RoleChild  Role = "CHILD"
	RoleAdmin  Role = "ADMIN"
)

// QuestionType represents the type of question
type QuestionType string

const (
	QuestionMC    QuestionType = "mc"
	QuestionType_ QuestionType = "type"
	QuestionTap   QuestionType = "tap"
	QuestionWord  QuestionType = "word"
	QuestionMatch QuestionType = "match"
	QuestionDrag  QuestionType = "drag"
	QuestionAudio QuestionType = "audio"
)

// AttemptStatus represents lesson attempt states
type AttemptStatus string

const (
	AttemptInProgress AttemptStatus = "IN_PROGRESS"
	AttemptCompleted  AttemptStatus = "COMPLETED"
	AttemptAbandoned  AttemptStatus = "ABANDONED"
)

// TransactionType for screen time
type TransactionType string

const (
	TransactionEarned  TransactionType = "EARNED"
	TransactionSpent   TransactionType = "SPENT"
	TransactionBonus   TransactionType = "BONUS"
	TransactionExpired TransactionType = "EXPIRED"
)

// --- Core Entities ---

type User struct {
	ID                 string    `json:"id" db:"id"`
	Email              string    `json:"email" db:"email"`
	PasswordHash       string    `json:"-" db:"password_hash"`
	Role               Role      `json:"role" db:"role"`
	LanguagePreference string    `json:"language_preference" db:"language_preference"`
	Timezone           string    `json:"timezone" db:"timezone"`
	CreatedAt          time.Time `json:"created_at" db:"created_at"`
	UpdatedAt          time.Time `json:"updated_at" db:"updated_at"`
}

type Family struct {
	ID        string    `json:"id" db:"id"`
	Name      string    `json:"name" db:"name"`
	CreatedAt time.Time `json:"created_at" db:"created_at"`
}

type FamilyMember struct {
	ID       string `json:"id" db:"id"`
	FamilyID string `json:"family_id" db:"family_id"`
	UserID   string `json:"user_id" db:"user_id"`
	Role     Role   `json:"role" db:"role"`
}

type InviteCode struct {
	ID        string    `json:"id" db:"id"`
	FamilyID  string    `json:"family_id" db:"family_id"`
	Code      string    `json:"code" db:"code"`
	ExpiresAt time.Time `json:"expires_at" db:"expires_at"`
	Used      bool      `json:"used" db:"used"`
}

type ChildProfile struct {
	ID                     string    `json:"id" db:"id"`
	UserID                 string    `json:"user_id" db:"user_id"`
	DisplayName            string    `json:"display_name" db:"display_name"`
	Avatar                 string    `json:"avatar" db:"avatar"`
	DateOfBirth            string    `json:"date_of_birth" db:"date_of_birth"`
	GradeLevel             int       `json:"grade_level" db:"grade_level"`
	XPTotal                int       `json:"xp_total" db:"xp_total"`
	CurrentStreak          int       `json:"current_streak" db:"current_streak"`
	LongestStreak          int       `json:"longest_streak" db:"longest_streak"`
	LastActiveDate         string    `json:"last_active_date" db:"last_active_date"`
	ScreenTimeBalanceSecs  int       `json:"screen_time_balance_seconds" db:"screen_time_balance_seconds"`
	CreatedAt              time.Time `json:"created_at" db:"created_at"`
}

type Subject struct {
	ID          string   `json:"id" db:"id"`
	Name        I18nText `json:"name" db:"name"`
	Icon        string   `json:"icon" db:"icon"`
	ColorScheme string   `json:"color_scheme" db:"color_scheme"`
	IsActive    bool     `json:"is_active" db:"is_active"`
}

type Lesson struct {
	ID             string   `json:"id" db:"id"`
	SubjectID      string   `json:"subject_id" db:"subject_id"`
	Title          I18nText `json:"title" db:"title"`
	Description    I18nText `json:"description" db:"description"`
	GradeLevel     int      `json:"grade_level" db:"grade_level"`
	DifficultyTier int      `json:"difficulty_tier" db:"difficulty_tier"`
	SortOrder      int      `json:"sort_order" db:"sort_order"`
	Questions      []Question `json:"questions,omitempty"`
}

type Question struct {
	ID               string          `json:"id" db:"id"`
	LessonID         string          `json:"lesson_id" db:"lesson_id"`
	Type             QuestionType    `json:"type" db:"type"`
	Prompt           I18nText        `json:"prompt" db:"prompt"`
	MediaURL         string          `json:"media_url,omitempty" db:"media_url"`
	Options          json.RawMessage `json:"options,omitempty" db:"options"`
	CorrectAnswer    json.RawMessage `json:"correct_answer" db:"correct_answer"`
	Pairs            json.RawMessage `json:"pairs,omitempty" db:"pairs"`
	DifficultyScore  int             `json:"difficulty_score" db:"difficulty_score"`
	TimeLimitSeconds int             `json:"time_limit_seconds,omitempty" db:"time_limit_seconds"`
	SortOrder        int             `json:"sort_order" db:"sort_order"`
}

type LessonAttempt struct {
	ID          string        `json:"id" db:"id"`
	ChildID     string        `json:"child_id" db:"child_id"`
	LessonID    string        `json:"lesson_id" db:"lesson_id"`
	StartedAt   time.Time     `json:"started_at" db:"started_at"`
	CompletedAt *time.Time    `json:"completed_at,omitempty" db:"completed_at"`
	Status      AttemptStatus `json:"status" db:"status"`
	Score       int           `json:"score" db:"score"`
	AccuracyPct float64      `json:"accuracy_pct" db:"accuracy_pct"`
	StarsEarned int           `json:"stars_earned" db:"stars_earned"`
	XPEarned    int           `json:"xp_earned" db:"xp_earned"`
}

type AttemptAnswer struct {
	ID          string `json:"id" db:"id"`
	AttemptID   string `json:"attempt_id" db:"attempt_id"`
	QuestionID  string `json:"question_id" db:"question_id"`
	GivenAnswer string `json:"given_answer" db:"given_answer"`
	IsCorrect   bool   `json:"is_correct" db:"is_correct"`
	TimeSpentMs int    `json:"time_spent_ms" db:"time_spent_ms"`
}

type ScreenTimeConfig struct {
	ID              string `json:"id" db:"id"`
	FamilyID        string `json:"family_id" db:"family_id"`
	ChildID         string `json:"child_id" db:"child_id"`
	PointsPerMinute int    `json:"points_per_minute" db:"points_per_minute"`
	DailyMaxMinutes int    `json:"daily_max_minutes" db:"daily_max_minutes"`
	BedtimeStart    string `json:"bedtime_start,omitempty" db:"bedtime_start"`
	BedtimeEnd      string `json:"bedtime_end,omitempty" db:"bedtime_end"`
	BlockedApps     json.RawMessage `json:"blocked_apps" db:"blocked_apps"`
	AllowedApps     json.RawMessage `json:"allowed_apps" db:"allowed_apps"`
}

type ScreenTimeTransaction struct {
	ID            string          `json:"id" db:"id"`
	ChildID       string          `json:"child_id" db:"child_id"`
	Type          TransactionType `json:"type" db:"type"`
	AmountSeconds int             `json:"amount_seconds" db:"amount_seconds"`
	Source        string          `json:"source" db:"source"`
	CreatedAt     time.Time       `json:"created_at" db:"created_at"`
}
