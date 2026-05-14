package database

import (
	"fmt"
	"log"

	"github.com/jmoiron/sqlx"
)

func RunMigrations(db *sqlx.DB) error {
	log.Println("running database migrations...")

	migrations := []string{
		migrationCreateExtensions,
		migrationCreateUsers,
		migrationCreateFamilies,
		migrationCreateFamilyMembers,
		migrationCreateInviteCodes,
		migrationCreateChildProfiles,
		migrationCreateSubjects,
		migrationCreateLessons,
		migrationCreateQuestions,
		migrationCreateLessonAttempts,
		migrationCreateAttemptAnswers,
		migrationCreateScreenTimeConfig,
		migrationCreateScreenTimeTransactions,
	}

	for i, m := range migrations {
		if _, err := db.Exec(m); err != nil {
			return fmt.Errorf("migration %d failed: %w", i+1, err)
		}
	}

	log.Println("migrations completed successfully")
	return nil
}

var migrationCreateExtensions = `
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
`

var migrationCreateUsers = `
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('PARENT', 'CHILD', 'ADMIN')),
    language_preference TEXT NOT NULL DEFAULT 'en',
    timezone TEXT NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
`

var migrationCreateFamilies = `
CREATE TABLE IF NOT EXISTS families (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
`

var migrationCreateFamilyMembers = `
CREATE TABLE IF NOT EXISTS family_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('PARENT', 'CHILD')),
    UNIQUE(family_id, user_id)
);
`

var migrationCreateInviteCodes = `
CREATE TABLE IF NOT EXISTS invite_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    code TEXT UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);
`

var migrationCreateChildProfiles = `
CREATE TABLE IF NOT EXISTS child_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL,
    avatar TEXT NOT NULL DEFAULT '',
    date_of_birth DATE,
    grade_level INT NOT NULL DEFAULT 1,
    xp_total INT NOT NULL DEFAULT 0,
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    last_active_date DATE,
    screen_time_balance_seconds INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
`

var migrationCreateSubjects = `
CREATE TABLE IF NOT EXISTS subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name JSONB NOT NULL,
    icon TEXT NOT NULL DEFAULT '',
    color_scheme TEXT NOT NULL DEFAULT '',
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
`

var migrationCreateLessons = `
CREATE TABLE IF NOT EXISTS lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    title JSONB NOT NULL,
    description JSONB NOT NULL DEFAULT '{}',
    grade_level INT NOT NULL DEFAULT 1,
    difficulty_tier INT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_lessons_subject ON lessons(subject_id);
CREATE INDEX IF NOT EXISTS idx_lessons_grade ON lessons(grade_level);
`

var migrationCreateQuestions = `
CREATE TABLE IF NOT EXISTS questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('mc', 'type', 'tap', 'word', 'match', 'drag', 'audio')),
    prompt JSONB NOT NULL,
    media_url TEXT,
    options JSONB,
    correct_answer JSONB NOT NULL,
    pairs JSONB,
    difficulty_score INT NOT NULL DEFAULT 1,
    time_limit_seconds INT,
    sort_order INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_questions_lesson ON questions(lesson_id);
`

var migrationCreateLessonAttempts = `
CREATE TABLE IF NOT EXISTS lesson_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id UUID NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    score INT NOT NULL DEFAULT 0,
    accuracy_pct REAL NOT NULL DEFAULT 0,
    stars_earned INT NOT NULL DEFAULT 0,
    xp_earned INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_attempts_child ON lesson_attempts(child_id);
`

var migrationCreateAttemptAnswers = `
CREATE TABLE IF NOT EXISTS attempt_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES lesson_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    given_answer TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    time_spent_ms INT NOT NULL DEFAULT 0
);
`

var migrationCreateScreenTimeConfig = `
CREATE TABLE IF NOT EXISTS screen_time_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID REFERENCES families(id) ON DELETE CASCADE,
    child_id UUID UNIQUE NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    points_per_minute INT NOT NULL DEFAULT 10,
    daily_max_minutes INT NOT NULL DEFAULT 120,
    bedtime_start TIME,
    bedtime_end TIME,
    blocked_apps JSONB NOT NULL DEFAULT '[]',
    allowed_apps JSONB NOT NULL DEFAULT '[]'
);
`

var migrationCreateScreenTimeTransactions = `
CREATE TABLE IF NOT EXISTS screen_time_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id UUID NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('EARNED', 'SPENT', 'BONUS', 'EXPIRED')),
    amount_seconds INT NOT NULL,
    source TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_stt_child ON screen_time_transactions(child_id);
`

