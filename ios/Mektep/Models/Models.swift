import Foundation

typealias I18nText = [String: String]

// MARK: - Auth
struct RegisterRequest: Codable {
    let email: String
    let password: String
    let role: String
    let languagePreference: String
    let displayName: String?
    let gradeLevel: Int?

    enum CodingKeys: String, CodingKey {
        case email, password, role
        case languagePreference = "language_preference"
        case displayName = "display_name"
        case gradeLevel = "grade_level"
    }
}

struct LoginRequest: Codable {
    let email: String
    let password: String
}

struct AuthResponse: Codable {
    let accessToken: String
    let refreshToken: String
    let user: User

    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case refreshToken = "refresh_token"
        case user
    }
}

struct User: Codable {
    let id: String
    let email: String
    let role: String
    let languagePreference: String
    let timezone: String

    enum CodingKeys: String, CodingKey {
        case id, email, role, timezone
        case languagePreference = "language_preference"
    }
}

// MARK: - Family
struct Family: Codable {
    let id: String
    let name: String
    let createdAt: String

    enum CodingKeys: String, CodingKey {
        case id, name
        case createdAt = "created_at"
    }
}

struct InviteCode: Codable {
    let id: String
    let familyId: String
    let code: String
    let expiresAt: String

    enum CodingKeys: String, CodingKey {
        case id, code
        case familyId = "family_id"
        case expiresAt = "expires_at"
    }
}

// MARK: - Child
struct ChildProfile: Codable {
    let id: String
    let userId: String
    let displayName: String
    let avatar: String
    let gradeLevel: Int
    let xpTotal: Int
    let currentStreak: Int
    let longestStreak: Int
    let screenTimeBalanceSeconds: Int

    enum CodingKeys: String, CodingKey {
        case id, avatar
        case userId = "user_id"
        case displayName = "display_name"
        case gradeLevel = "grade_level"
        case xpTotal = "xp_total"
        case currentStreak = "current_streak"
        case longestStreak = "longest_streak"
        case screenTimeBalanceSeconds = "screen_time_balance_seconds"
    }
}

// MARK: - Subjects & Lessons
struct Subject: Codable, Identifiable {
    let id: String
    let name: I18nText
    let icon: String
    let colorScheme: String
    let isActive: Bool

    enum CodingKeys: String, CodingKey {
        case id, name, icon
        case colorScheme = "color_scheme"
        case isActive = "is_active"
    }
}

struct Lesson: Codable, Identifiable {
    let id: String
    let subjectId: String
    let title: I18nText
    let description: I18nText
    let gradeLevel: Int
    let difficultyTier: Int
    let sortOrder: Int
    var questions: [Question]

    enum CodingKeys: String, CodingKey {
        case id, title, description, questions
        case subjectId = "subject_id"
        case gradeLevel = "grade_level"
        case difficultyTier = "difficulty_tier"
        case sortOrder = "sort_order"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(String.self, forKey: .id)
        subjectId = try c.decode(String.self, forKey: .subjectId)
        title = try c.decode(I18nText.self, forKey: .title)
        description = try c.decodeIfPresent(I18nText.self, forKey: .description) ?? [:]
        gradeLevel = try c.decode(Int.self, forKey: .gradeLevel)
        difficultyTier = try c.decode(Int.self, forKey: .difficultyTier)
        sortOrder = try c.decode(Int.self, forKey: .sortOrder)
        questions = try c.decodeIfPresent([Question].self, forKey: .questions) ?? []
    }
}

struct Question: Codable, Identifiable {
    let id: String
    let type: String
    let prompt: I18nText
    let mediaUrl: String?
    let options: AnyCodable?
    let correctAnswer: AnyCodable?
    let pairs: AnyCodable?
    let difficultyScore: Int
    let timeLimitSeconds: Int?
    let sortOrder: Int

    enum CodingKeys: String, CodingKey {
        case id, type, prompt, options, pairs
        case mediaUrl = "media_url"
        case correctAnswer = "correct_answer"
        case difficultyScore = "difficulty_score"
        case timeLimitSeconds = "time_limit_seconds"
        case sortOrder = "sort_order"
    }
}

// MARK: - Attempts
struct LessonAttempt: Codable {
    let id: String
    let childId: String
    let lessonId: String
    let startedAt: String
    let status: String

    enum CodingKeys: String, CodingKey {
        case id, status
        case childId = "child_id"
        case lessonId = "lesson_id"
        case startedAt = "started_at"
    }
}

struct SubmitAnswerRequest: Codable {
    let questionId: String
    let givenAnswer: String
    let timeSpentMs: Int

    enum CodingKeys: String, CodingKey {
        case questionId = "question_id"
        case givenAnswer = "given_answer"
        case timeSpentMs = "time_spent_ms"
    }
}

struct SubmitAnswerResponse: Codable {
    let isCorrect: Bool
    let correctAnswer: String?

    enum CodingKeys: String, CodingKey {
        case isCorrect = "is_correct"
        case correctAnswer = "correct_answer"
    }
}

struct CompleteResponse: Codable {
    let score: Int
    let accuracyPct: Double
    let starsEarned: Int
    let xpEarned: Int

    enum CodingKeys: String, CodingKey {
        case score
        case accuracyPct = "accuracy_pct"
        case starsEarned = "stars_earned"
        case xpEarned = "xp_earned"
    }
}

struct DashboardResponse: Codable {
    let profile: ChildProfile
    let progress: [SubjectProgress]
}

struct SubjectProgress: Codable {
    let subjectId: String
    let completedLessons: Int
    let totalLessons: Int
    let bestStars: Int
    let totalXp: Int

    enum CodingKeys: String, CodingKey {
        case subjectId = "subject_id"
        case completedLessons = "completed_lessons"
        case totalLessons = "total_lessons"
        case bestStars = "best_stars"
        case totalXp = "total_xp"
    }
}

// MARK: - Screen Time
struct ScreenTimeBalance: Codable {
    let balanceSeconds: Int
    let balanceMinutes: Int

    enum CodingKeys: String, CodingKey {
        case balanceSeconds = "balance_seconds"
        case balanceMinutes = "balance_minutes"
    }
}

struct ScreenTimeConfig: Codable {
    let id: String
    let childId: String
    let pointsPerMinute: Int
    let dailyMaxMinutes: Int

    enum CodingKeys: String, CodingKey {
        case id
        case childId = "child_id"
        case pointsPerMinute = "points_per_minute"
        case dailyMaxMinutes = "daily_max_minutes"
    }
}

// MARK: - AnyCodable helper for dynamic JSON
struct AnyCodable: Codable {
    let value: Any

    init(_ value: Any) { self.value = value }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let int = try? container.decode(Int.self) { value = int }
        else if let double = try? container.decode(Double.self) { value = double }
        else if let string = try? container.decode(String.self) { value = string }
        else if let bool = try? container.decode(Bool.self) { value = bool }
        else if let array = try? container.decode([AnyCodable].self) { value = array.map(\.value) }
        else if let dict = try? container.decode([String: AnyCodable].self) { value = dict.mapValues(\.value) }
        else if container.decodeNil() { value = NSNull() }
        else { throw DecodingError.dataCorruptedError(in: container, debugDescription: "Unsupported type") }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch value {
        case let v as Int: try container.encode(v)
        case let v as Double: try container.encode(v)
        case let v as String: try container.encode(v)
        case let v as Bool: try container.encode(v)
        case is NSNull: try container.encodeNil()
        default: try container.encodeNil()
        }
    }
}
