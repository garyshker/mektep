import Foundation

class APIClient {
    static let shared = APIClient()

    #if DEBUG
    private let baseURL = "http://localhost:8080/api/v1"
    #else
    private let baseURL = "https://api.mektep.app/api/v1"
    #endif

    private let session = URLSession.shared
    private let decoder: JSONDecoder = {
        let d = JSONDecoder()
        return d
    }()
    private let encoder = JSONEncoder()

    var accessToken: String? {
        get { UserDefaults.standard.string(forKey: "access_token") }
        set { UserDefaults.standard.set(newValue, forKey: "access_token") }
    }

    var refreshToken: String? {
        get { UserDefaults.standard.string(forKey: "refresh_token") }
        set { UserDefaults.standard.set(newValue, forKey: "refresh_token") }
    }

    var userId: String? {
        get { UserDefaults.standard.string(forKey: "user_id") }
        set { UserDefaults.standard.set(newValue, forKey: "user_id") }
    }

    var userRole: String? {
        get { UserDefaults.standard.string(forKey: "user_role") }
        set { UserDefaults.standard.set(newValue, forKey: "user_role") }
    }

    var childId: String? {
        get { UserDefaults.standard.string(forKey: "child_id") }
        set { UserDefaults.standard.set(newValue, forKey: "child_id") }
    }

    var language: String {
        get { UserDefaults.standard.string(forKey: "language") ?? "en" }
        set { UserDefaults.standard.set(newValue, forKey: "language") }
    }

    // MARK: - Generic Request

    func request<T: Decodable>(_ method: String, path: String, body: (any Encodable)? = nil) async throws -> T {
        guard let url = URL(string: baseURL + path) else {
            throw APIError.invalidURL
        }

        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if let token = accessToken, !path.contains("/auth/") {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let body = body {
            req.httpBody = try encoder.encode(AnyEncodable(body))
        }

        let (data, response) = try await session.data(for: req)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.networkError
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            let errorBody = String(data: data, encoding: .utf8) ?? ""
            throw APIError.serverError(httpResponse.statusCode, errorBody)
        }

        return try decoder.decode(T.self, from: data)
    }

    // MARK: - Auth

    func register(_ request: RegisterRequest) async throws -> AuthResponse {
        let response: AuthResponse = try await self.request("POST", path: "/auth/register", body: request)
        saveAuth(response)
        return response
    }

    func login(_ request: LoginRequest) async throws -> AuthResponse {
        let response: AuthResponse = try await self.request("POST", path: "/auth/login", body: request)
        saveAuth(response)
        return response
    }

    func logout() {
        accessToken = nil
        refreshToken = nil
        userId = nil
        userRole = nil
        childId = nil
    }

    private func saveAuth(_ response: AuthResponse) {
        accessToken = response.accessToken
        refreshToken = response.refreshToken
        userId = response.user.id
        userRole = response.user.role
    }

    // MARK: - Subjects & Lessons

    func getSubjects() async throws -> [Subject] {
        try await request("GET", path: "/subjects")
    }

    func getLessons(subjectId: String) async throws -> [Lesson] {
        try await request("GET", path: "/subjects/\(subjectId)/lessons")
    }

    func getLesson(id: String) async throws -> Lesson {
        try await request("GET", path: "/lessons/\(id)")
    }

    // MARK: - Attempts

    func startAttempt(lessonId: String, childId: String) async throws -> LessonAttempt {
        try await request("POST", path: "/lessons/\(lessonId)/start", body: ["child_id": childId])
    }

    func submitAnswer(attemptId: String, request: SubmitAnswerRequest) async throws -> SubmitAnswerResponse {
        try await self.request("POST", path: "/lesson-attempts/\(attemptId)/answer", body: request)
    }

    func completeAttempt(attemptId: String) async throws -> CompleteResponse {
        try await request("POST", path: "/lesson-attempts/\(attemptId)/complete")
    }

    // MARK: - Dashboard

    func getDashboard(childId: String) async throws -> DashboardResponse {
        try await request("GET", path: "/children/\(childId)/dashboard")
    }

    // MARK: - Screen Time

    func getScreenTimeBalance(childId: String) async throws -> ScreenTimeBalance {
        try await request("GET", path: "/children/\(childId)/screen-time/balance")
    }
}

// MARK: - Errors

enum APIError: LocalizedError {
    case invalidURL
    case networkError
    case serverError(Int, String)

    var errorDescription: String? {
        switch self {
        case .invalidURL: return "Invalid URL"
        case .networkError: return "Network error"
        case .serverError(let code, let msg): return "Server error \(code): \(msg)"
        }
    }
}

// MARK: - AnyEncodable wrapper

struct AnyEncodable: Encodable {
    private let _encode: (Encoder) throws -> Void

    init(_ value: any Encodable) {
        _encode = { encoder in try value.encode(to: encoder) }
    }

    func encode(to encoder: Encoder) throws {
        try _encode(encoder)
    }
}
