import Foundation
import SwiftUI

@MainActor
class AuthManager: ObservableObject {
    @Published var isLoggedIn: Bool = false
    @Published var userRole: String = ""
    @Published var isLoading: Bool = false
    @Published var error: String?

    private let api = APIClient.shared

    init() {
        isLoggedIn = api.accessToken != nil
        userRole = api.userRole ?? ""
    }

    func login(email: String, password: String) async {
        isLoading = true
        error = nil
        do {
            let response = try await api.login(LoginRequest(email: email, password: password))
            isLoggedIn = true
            userRole = response.user.role
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func register(email: String, password: String, role: String, language: String, displayName: String?, gradeLevel: Int?) async {
        isLoading = true
        error = nil
        do {
            let request = RegisterRequest(
                email: email, password: password, role: role,
                languagePreference: language, displayName: displayName, gradeLevel: gradeLevel
            )
            let response = try await api.register(request)
            isLoggedIn = true
            userRole = response.user.role
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func logout() {
        api.logout()
        isLoggedIn = false
        userRole = ""
    }
}
