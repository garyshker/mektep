# Mektep - Educational Screen-Time Management Platform
## Professional Project Specification & Development Prompt

---

## 1. Executive Summary

**Project Name:** Mektep (from Kazakh "мектеп" - school)

**Vision:** A mobile-first educational platform that transforms children's screen time from a parental battleground into a structured learning incentive system. Instead of prohibiting phone usage - an approach that fails against modern reality - Mektep requires children to earn screen time by completing educational exercises in math, language, science, and other subjects through gamified, age-appropriate lessons.

**Core Premise:** Children cannot open recreational apps (games, social media, video streaming) until they earn sufficient points through in-app learning. Points translate directly into minutes of unrestricted device usage. The app acts as both an educational tool and a device-level app blocker/usage manager.

**Target Audience:**
- **Primary (Phase 1):** Children aged 6-14 and their parents/guardians
- **Secondary (Phase 2):** Teenagers 15-17, adult learners, language learners of all ages

**Reference Design:** [github.com/garyshker/mektep](https://github.com/garyshker/mektep) - a React-based interactive prototype featuring a Duolingo-inspired UI with subject cards, XP/streak gamification, a lesson runner with 6 question types, and trilingual support (Kazakh, Russian, English). This prototype establishes the visual language and interaction patterns to be refined and elevated for production.

---

## 2. Problem Statement

Modern children spend 4-7+ hours daily on smartphones. Outright prohibition is ineffective and creates conflict. Parents need a tool that:

1. **Does not fight the inevitable** - accepts that children will use devices
2. **Creates a fair exchange** - learning earns entertainment time
3. **Gives parents control** - configurable rules, visibility into progress, remote management
4. **Makes learning enjoyable** - gamification ensures children are intrinsically motivated, not just coerced
5. **Works at the OS level** - actually blocks apps, not just suggests limits (requires device-level permissions on Android and iOS)

---

## 3. Design Direction

### 3.1 Existing Prototype Strengths (to preserve)
- Clean, green-branded color scheme (`#0E8C6B`) with subject-specific accent colors
- Nunito typography for warmth and readability
- Card-based dashboard with progress visualization (stars, progress bars, XP rings)
- Gamification system: XP, streaks, hearts/lives, star ratings, daily quests
- Six interactive question types: multiple choice, typed input, tap-to-select, word problems, matching pairs, fill-in-the-blank
- Audio feedback via Web Audio API (correct/incorrect/completion sounds)
- Trilingual UI (Kazakh, Russian, English)
- Lesson resume from localStorage with 24-hour TTL

### 3.2 Design Improvements Required
- **Modernize visual design:** Elevate from prototype-level to production-quality. Add micro-animations, smoother transitions, polished illustrations, and a more refined component library. Consider a dedicated mascot character (the prototype uses an otter emoji) with proper illustrations
- **Improve information hierarchy:** The dashboard currently presents all elements with equal weight. Prioritize the "continue learning" CTA and earned screen-time balance
- **Add parent-facing UI:** The prototype has no parent/admin interface. Design a separate, mature parent dashboard for configuration, monitoring, and reporting
- **Accessibility:** Ensure WCAG 2.1 AA compliance, support for larger text sizes, high-contrast mode, and screen reader compatibility
- **Onboarding flow:** Add a guided first-run experience for both child and parent accounts
- **Screen-time visualization:** Make earned time and remaining balance prominent and intuitive (countdown timer, progress-to-goal bar)
- **Dark mode support:** Children often use devices at night
- **Haptic feedback on mobile:** Complement audio cues with tactile feedback
- **Adaptive difficulty indicators:** Visual cues showing when content is adjusting to the child's level

---

## 4. Core Features

### 4.1 Learning Engine
| Feature | Description |
|---------|-------------|
| **Subject catalog** | Math, native language (Kazakh, Russian, etc.), English, World Studies/Science. Extensible to new subjects |
| **Structured curriculum** | Lessons organized by subject, grade level, and difficulty tier. Each lesson contains 8-12 questions |
| **Question types** | Multiple choice, typed answer, tap-to-select, word problems, matching pairs, drag-and-drop ordering, image-based questions, listen-and-answer (audio) |
| **Adaptive difficulty** | Algorithm adjusts question difficulty based on accuracy rate, response time, and historical performance |
| **Spaced repetition** | Incorrectly answered questions resurface at optimized intervals |
| **Gamification** | XP points, daily streaks, heart/lives system (3 hearts per session), star ratings (1-3 per lesson), achievement badges, weekly challenges |
| **Audio feedback** | Distinct sounds for correct answers, wrong answers, and lesson completion. Optional text-to-speech for younger children |
| **Lesson resume** | Save mid-lesson progress; resume within 24 hours |

### 4.2 Screen-Time Economy
| Feature | Description |
|---------|-------------|
| **Point earning** | Completing lessons earns points. Bonus points for accuracy, speed, streaks, and perfect scores |
| **Point-to-time conversion** | Configurable by parent (e.g., 100 points = 30 minutes). Different rates for weekdays vs. weekends |
| **Time balance** | Persistent balance of earned screen time. Visible to both child and parent |
| **Active timer** | When child switches to a recreational app, timer counts down from earned balance |
| **Time expiration** | Optional: unused earned time expires at end of day or carries over (parent-configurable) |
| **Bonus time** | Parents can grant bonus time manually (reward for chores, good behavior, etc.) |
| **Emergency override** | Parents can temporarily unlock all apps (e.g., child needs to make a call) |

### 4.3 App Blocking & Device Management
| Feature | Description |
|---------|-------------|
| **App categorization** | Auto-categorize installed apps as "educational," "communication," "entertainment," "social media," etc. Parent can recategorize |
| **Blocklist / allowlist** | Parent defines which apps require earned time and which are always available (phone, messaging, emergency) |
| **App launch interception** | When a blocked app is opened and balance is zero, redirect to Mektep with a friendly "earn more time" prompt |
| **Usage tracking** | Log which apps are used and for how long. Daily/weekly reports for parents |
| **Bedtime mode** | Scheduled blackout periods where only allowlisted apps work |
| **Android implementation** | Usage Access permission + Accessibility Service or Device Admin API to overlay/block apps |
| **iOS implementation** | Screen Time API (Family Controls / DeviceActivityMonitor framework) via the `FamilyControls` and `ManagedSettings` frameworks introduced in iOS 15+ |

### 4.4 Parent Dashboard
| Feature | Description |
|---------|-------------|
| **Child profiles** | Manage multiple children from a single parent account |
| **Progress reports** | Per-subject performance, time spent learning, accuracy trends, streak history |
| **Screen-time reports** | Which apps were used, for how long, daily/weekly summaries |
| **Configuration** | Set point-to-time ratios, bedtime schedules, app allowlists/blocklists, daily learning goals |
| **Remote management** | Push configuration changes from parent's device to child's device in real time |
| **Notifications** | Alerts when child completes a milestone, when screen time runs out, when daily goals are met |
| **Family linking** | QR code or invite-code-based linking between parent and child accounts |

### 4.5 Multi-Language Support
| Priority | Languages |
|----------|-----------|
| **Launch (Phase 1)** | Kazakh, Russian, English |
| **Phase 2** | Turkish, Uzbek, Kyrgyz |
| **Phase 3** | Arabic, Chinese (Mandarin), Spanish, French, Hindi |

Both the **app UI** and **lesson content** must be independently localizable. A child studying in Kazakh should be able to have the app interface in Russian, for example.

---

## 5. Technical Architecture

### 5.1 Development Phases

```
Phase 1: Backend API + Admin Panel          (Months 1-3)
Phase 2: Android App                        (Months 3-5)
Phase 3: iOS App                            (Months 5-7)
Phase 4: Advanced features + scaling        (Months 7-9)
```

### 5.2 Backend (Cost-Optimized / Serverless-First)

**Design Principle:** No always-on VMs. Pay only for what you use. At low user counts, monthly infrastructure cost should be **< $5-10/month** and scale linearly with usage.

| Component | Technology | Cost Rationale |
|-----------|------------|----------------|
| **Language/Framework** | Go (with Gin/Chi) or Node.js/TypeScript | Lightweight, fast cold starts for serverless |
| **Deployment** | **AWS Lambda + API Gateway** (or GCP Cloud Functions + Cloud Run on-demand) | Zero cost at idle. Pay per request (~$0.20 per 1M requests) |
| **API style** | RESTful JSON API with OpenAPI 3.0 spec | Simple, cacheable |
| **Authentication** | Firebase Auth (free tier: 50K MAU) or Supabase Auth | Eliminates custom auth infrastructure |
| **Database** | **Supabase (PostgreSQL)** free tier (500MB, 50K MAU) or **PlanetScale** (MySQL, free tier) or **Neon** (serverless Postgres, free tier with auto-suspend) | Auto-sleep when idle, no cost at rest |
| **Cache** | **Upstash Redis** (serverless, free tier: 10K commands/day) | Pay-per-request Redis, no always-on instance |
| **Object storage** | AWS S3 or Cloudflare R2 (free egress) for lesson media | Pennies per GB |
| **Push notifications** | Firebase Cloud Messaging (free) + APNs | No cost |
| **Real-time sync** | Firebase Realtime Database or Supabase Realtime (free tier) for parent-child config sync | No WebSocket server to maintain |
| **Content management** | **CLI tool for JSON lesson upload** (see 5.7). No admin panel needed initially | Zero infrastructure |
| **Analytics** | Events logged to database. Simple queries. No separate analytics pipeline initially | Reuse existing DB |
| **CDN** | Cloudflare (free tier) in front of API Gateway | Free caching, DDoS protection |
| **CI/CD** | GitHub Actions (free for public repos, 2000 min/mo for private) | Minimal cost |

**Estimated monthly cost at launch (< 1K users):** $0-10/month
**At 10K users:** ~$20-50/month
**At 100K users:** Evaluate migration to containers (Cloud Run / ECS) when serverless costs exceed container costs

### 5.3 Data Model (Core Entities)

```
User
  ├── id, email, phone, password_hash, role (PARENT | CHILD | ADMIN)
  ├── language_preference, timezone
  └── created_at, updated_at

Family
  ├── id, name
  └── parent_users[], child_users[]

ChildProfile
  ├── id, user_id, display_name, avatar, date_of_birth, grade_level
  ├── xp_total, current_streak, longest_streak, last_active_date
  └── screen_time_balance_seconds

Subject
  ├── id, name (i18n), icon, color_scheme
  └── grade_levels[], is_active

Lesson
  ├── id, subject_id, title (i18n), description (i18n)
  ├── grade_level, difficulty_tier, sort_order
  └── questions[]

Question
  ├── id, lesson_id, type (MC | TYPE | TAP | WORD | MATCH | DRAG | AUDIO)
  ├── prompt (i18n), media_url, options (i18n), correct_answer
  ├── difficulty_score, time_limit_seconds
  └── hints (i18n)[]

LessonAttempt
  ├── id, child_id, lesson_id
  ├── started_at, completed_at, status (IN_PROGRESS | COMPLETED | ABANDONED)
  ├── score, accuracy_pct, stars_earned, xp_earned
  └── answers[] { question_id, given_answer, is_correct, time_spent_ms }

ScreenTimeConfig
  ├── id, family_id, child_id
  ├── points_per_minute, daily_max_minutes
  ├── weekday_schedule, weekend_schedule
  ├── bedtime_start, bedtime_end
  └── blocked_apps[], allowed_apps[]

ScreenTimeTransaction
  ├── id, child_id, type (EARNED | SPENT | BONUS | EXPIRED)
  ├── amount_seconds, source (lesson_id | parent_grant | system)
  └── created_at

AppUsageLog
  ├── id, child_id, app_package_name, app_display_name
  ├── started_at, ended_at, duration_seconds
  └── category
```

### 5.4 API Endpoints (Key Groups)

```
Auth:
  POST   /api/v1/auth/register
  POST   /api/v1/auth/login
  POST   /api/v1/auth/refresh
  POST   /api/v1/auth/forgot-password

Family:
  POST   /api/v1/families
  POST   /api/v1/families/{id}/invite        (generate invite code)
  POST   /api/v1/families/{id}/join           (child joins via code)
  GET    /api/v1/families/{id}/members

Child Profile:
  GET    /api/v1/children/{id}/dashboard      (progress, balance, quests)
  GET    /api/v1/children/{id}/progress       (per-subject stats)
  GET    /api/v1/children/{id}/screen-time    (current balance + history)

Lessons:
  GET    /api/v1/subjects                     (list subjects for grade)
  GET    /api/v1/subjects/{id}/lessons        (lesson catalog)
  GET    /api/v1/lessons/{id}                 (full lesson with questions)
  POST   /api/v1/lessons/{id}/start           (begin attempt)
  POST   /api/v1/lesson-attempts/{id}/answer  (submit answer)
  POST   /api/v1/lesson-attempts/{id}/complete

Screen Time:
  GET    /api/v1/children/{id}/screen-time/balance
  POST   /api/v1/children/{id}/screen-time/spend
  POST   /api/v1/children/{id}/screen-time/bonus   (parent grants time)
  
Parent Config:
  GET    /api/v1/children/{id}/config
  PUT    /api/v1/children/{id}/config
  GET    /api/v1/children/{id}/app-usage      (usage reports)
  POST   /api/v1/children/{id}/emergency-unlock

Admin / CMS:
  CRUD   /api/v1/admin/subjects
  CRUD   /api/v1/admin/lessons
  CRUD   /api/v1/admin/questions
```

### 5.7 Lesson Content Upload (CLI / JSON)

**No admin panel required initially.** Lessons and questions are authored as JSON files and uploaded via a CLI tool or direct API call. This eliminates the need for a content management UI and its associated hosting costs.

#### JSON Lesson Format

```json
{
  "subject": "math",
  "grade_level": 2,
  "difficulty_tier": 1,
  "sort_order": 3,
  "title": {
    "kk": "Қосу: 10-нан 50-ге дейін",
    "ru": "Сложение: от 10 до 50",
    "en": "Addition: 10 to 50"
  },
  "description": {
    "kk": "Екі санды қосуды үйреніңіз",
    "ru": "Научитесь складывать два числа",
    "en": "Learn to add two numbers"
  },
  "questions": [
    {
      "type": "mc",
      "prompt": { "kk": "12 + 15 = ?", "ru": "12 + 15 = ?", "en": "12 + 15 = ?" },
      "options": [
        { "kk": "25", "ru": "25", "en": "25" },
        { "kk": "27", "ru": "27", "en": "27" },
        { "kk": "30", "ru": "30", "en": "30" },
        { "kk": "22", "ru": "22", "en": "22" }
      ],
      "correct_answer": 1,
      "difficulty_score": 2,
      "time_limit_seconds": 30
    },
    {
      "type": "type",
      "prompt": { "kk": "18 + 7 = ?", "ru": "18 + 7 = ?", "en": "18 + 7 = ?" },
      "correct_answer": "25",
      "difficulty_score": 2,
      "time_limit_seconds": 20
    },
    {
      "type": "match",
      "prompt": { "kk": "Сәйкестендіріңіз", "ru": "Сопоставьте", "en": "Match the pairs" },
      "pairs": [
        { "left": "5 + 5", "right": "10" },
        { "left": "12 + 8", "right": "20" },
        { "left": "15 + 15", "right": "30" },
        { "left": "20 + 5", "right": "25" }
      ],
      "difficulty_score": 3
    }
  ]
}
```

#### CLI Upload Commands

```bash
# Upload a single lesson
mektep-cli lesson upload math_grade2_lesson3.json

# Upload all lessons in a directory
mektep-cli lesson upload ./lessons/math/grade2/

# Validate JSON without uploading
mektep-cli lesson validate math_grade2_lesson3.json

# List existing lessons for a subject
mektep-cli lesson list --subject=math --grade=2

# Delete a lesson by ID
mektep-cli lesson delete --id=abc123

# Export existing lessons to JSON (for editing)
mektep-cli lesson export --subject=math --grade=2 --output=./export/

# Bulk seed from curriculum pack
mektep-cli seed ./curriculum/grade1-pack.json
```

The CLI tool is a lightweight Go or Python script that authenticates with an admin API key and sends JSON payloads to the lesson management endpoints. It validates the JSON schema locally before uploading, checks for duplicate lessons, and reports upload results.

#### Upload API Endpoints (Admin-only, API key auth)

```
POST   /api/v1/admin/lessons/upload          (single lesson JSON)
POST   /api/v1/admin/lessons/upload/bulk      (array of lessons)
GET    /api/v1/admin/lessons/validate         (dry-run validation)
DELETE /api/v1/admin/lessons/{id}
GET    /api/v1/admin/lessons/export?subject=&grade=
```

---

### 5.5 Android App

| Component | Technology |
|-----------|------------|
| **Language** | Kotlin |
| **UI framework** | Jetpack Compose with Material 3 |
| **Architecture** | MVVM + Clean Architecture (Presentation / Domain / Data layers) |
| **Networking** | Retrofit + OkHttp + Kotlin Serialization |
| **Local database** | Room (SQLite) for offline lesson caching and progress |
| **DI** | Hilt (Dagger) |
| **App blocking** | `UsageStatsManager` + `AccessibilityService` to detect foreground app and overlay a "blocked" screen. Alternatively, Device Policy Controller for enterprise-grade control |
| **Background service** | Foreground service with persistent notification to track active screen time countdown |
| **Push** | Firebase Cloud Messaging |
| **Min SDK** | API 26 (Android 8.0) |

### 5.6 iOS App

| Component | Technology |
|-----------|------------|
| **Language** | Swift |
| **UI framework** | SwiftUI with iOS 16+ deployment target |
| **Architecture** | MVVM + Swift Concurrency (async/await, Actors) |
| **Networking** | URLSession + Codable |
| **Local database** | SwiftData or Core Data for offline caching |
| **App blocking** | `FamilyControls` framework + `DeviceActivityMonitor` + `ManagedSettings` (requires Screen Time API entitlement from Apple) |
| **Push** | APNs via Firebase or native |
| **Deployment** | iOS 16+ (FamilyControls requirement) |

> **Note on iOS App Blocking:** Apple's Screen Time API requires a special entitlement (`com.apple.developer.family-controls`). The app must request Family Controls authorization from the parent. This is the only sanctioned way to block apps on iOS. Plan for the Apple review process.

---

## 6. Non-Functional Requirements

| Requirement | Target |
|-------------|--------|
| **API response time** | < 200ms p95 for lesson/question endpoints |
| **Offline support** | Lessons must be playable offline (pre-cached). Sync when reconnected |
| **Concurrent users** | Design for 10K concurrent initially, scalable to 100K+ |
| **Data privacy** | COPPA-compliant (children's data). GDPR-compliant for EU users. Minimal data collection |
| **Security** | All traffic over TLS. Passwords hashed with bcrypt/argon2. Parent accounts require email verification. Child accounts cannot be created without parent consent |
| **Uptime** | 99.9% availability target |
| **App size** | < 50MB initial download. Lesson content downloaded on demand |
| **Battery** | Background screen-time monitoring must be battery-efficient (< 3% daily drain) |
| **Accessibility** | WCAG 2.1 AA. Support Dynamic Type (iOS) and font scaling (Android). Screen reader labels on all interactive elements |

---

## 7. Monetization Model (Future Consideration)

| Tier | Features |
|------|----------|
| **Free** | 2 subjects, 3 lessons per subject, basic screen-time control, 1 child profile |
| **Premium ($4.99/mo)** | All subjects and lessons, unlimited children, advanced reports, custom curricula, bedtime scheduling, app usage analytics |
| **Family ($7.99/mo)** | Premium + up to 6 children, shared family leaderboard, teacher/tutor access |

---

## 8. Success Metrics

| Metric | Target |
|--------|--------|
| **Daily Active Users (children)** | Track and grow weekly |
| **Lessons completed per day per child** | 3+ |
| **Average session duration (learning)** | 15-20 minutes |
| **7-day retention** | > 60% |
| **30-day retention** | > 40% |
| **Parent satisfaction (NPS)** | > 50 |
| **Screen time reduction** | 20%+ decrease in recreational screen time within 30 days of use |

---

## 9. Development Sequence

### Phase 1: Backend Foundation (Serverless)
1. Project scaffolding (Go or Node.js), CI/CD pipeline via GitHub Actions
2. Deploy to AWS Lambda + API Gateway (or equivalent serverless)
3. Provision serverless Postgres (Neon/Supabase) + Upstash Redis
4. User authentication (Firebase Auth or Supabase Auth)
5. Family management and parent-child linking (invite codes)
6. Lesson JSON schema definition and validation
7. **CLI tool** for lesson upload/validate/export (`mektep-cli`)
8. Lesson and question serving endpoints (by subject, grade, difficulty)
9. Lesson attempt lifecycle (start, answer, complete, scoring)
10. XP and progression system (streaks, levels, badges)
11. Screen-time balance ledger (earn, spend, bonus, expire)
12. Screen-time configuration endpoints (parent-managed)
13. Push notification setup (Firebase Cloud Messaging)
14. Real-time parent-child sync (Supabase Realtime or Firebase)
15. Seed database with initial curriculum via CLI (math + Kazakh for grades 1-4)
16. OpenAPI documentation
17. Unit tests, integration tests

### Phase 2: Android App
1. Project setup (Compose, Hilt, Retrofit, Room)
2. Authentication screens (login, register, onboarding)
3. Child dashboard (subject grid, progress, XP, streaks)
4. Lesson runner (all question types, audio, animations)
5. Screen-time balance display and countdown
6. App blocking service (AccessibilityService / UsageStats)
7. Parent dashboard screens (reports, config, family management)
8. Offline lesson caching and sync
9. Push notifications
10. Settings, language selection, profile management
11. QA, performance optimization, accessibility audit

### Phase 3: iOS App
1. Project setup (SwiftUI, async/await, SwiftData)
2. Authentication and onboarding
3. Child dashboard and lesson runner (parity with Android)
4. FamilyControls integration for app blocking
5. Parent dashboard
6. Offline support and push notifications
7. QA, App Store review preparation (especially for Screen Time API entitlement)

---

## 10. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| **Apple rejects Screen Time API entitlement** | Apply early. Prepare documentation proving educational intent. Have a degraded mode that shows usage reports without blocking |
| **Android OEM fragmentation (battery optimization kills background service)** | Test on Samsung, Xiaomi, Huawei, Oppo. Use `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Provide user guidance for each OEM |
| **Children find workarounds** | Monitor for uninstall attempts. Use Device Admin to prevent removal. On iOS, require parent to remove app |
| **Content creation bottleneck** | Build robust CMS early. Consider AI-assisted question generation. Partner with educators for curriculum validation |
| **COPPA/GDPR compliance** | Consult legal counsel. Implement verifiable parental consent. Minimize data collection. No behavioral advertising |
| **Low engagement after novelty wears off** | Invest in social features (friend challenges, class leaderboards), seasonal events, new content drops, achievement systems |

---

*This specification is based on the interactive prototype at [github.com/garyshker/mektep](https://github.com/garyshker/mektep), which demonstrates the core learning UI with a green-themed card-based dashboard, 6 question types, XP/streak gamification, and trilingual support (Kazakh/Russian/English). The production system extends this foundation with device-level app blocking, a parent management layer, a backend API, and native Android/iOS implementations.*
