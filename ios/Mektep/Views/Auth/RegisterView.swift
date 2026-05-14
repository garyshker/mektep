import SwiftUI

struct RegisterView: View {
    @EnvironmentObject var authManager: AuthManager
    @Environment(\.dismiss) var dismiss
    @State private var email = ""
    @State private var password = ""
    @State private var displayName = ""
    @State private var role = "CHILD"
    @State private var language = "en"
    @State private var gradeLevel = "1"

    var body: some View {
        NavigationStack {
            Form {
                Section("I am a") {
                    Picker("Role", selection: $role) {
                        Text("Student").tag("CHILD")
                        Text("Parent").tag("PARENT")
                    }
                    .pickerStyle(.segmented)
                }

                Section("Account") {
                    TextField("Email", text: $email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                    SecureField("Password (6+ characters)", text: $password)
                }

                if role == "CHILD" {
                    Section("Student Info") {
                        TextField("Display Name", text: $displayName)
                        TextField("Grade Level (1-12)", text: $gradeLevel)
                            .keyboardType(.numberPad)
                    }
                }

                Section("Language") {
                    Picker("Preferred Language", selection: $language) {
                        Text("Qazaqsha").tag("kk")
                        Text("Русский").tag("ru")
                        Text("English").tag("en")
                    }
                    .pickerStyle(.segmented)
                }

                if let error = authManager.error {
                    Section {
                        Text(error).foregroundColor(.red)
                    }
                }

                Section {
                    Button {
                        Task {
                            await authManager.register(
                                email: email,
                                password: password,
                                role: role,
                                language: language,
                                displayName: displayName.isEmpty ? nil : displayName,
                                gradeLevel: Int(gradeLevel)
                            )
                            if authManager.isLoggedIn { dismiss() }
                        }
                    } label: {
                        if authManager.isLoading {
                            ProgressView().frame(maxWidth: .infinity)
                        } else {
                            Text("Create Account")
                                .frame(maxWidth: .infinity)
                                .bold()
                        }
                    }
                    .disabled(email.isEmpty || password.count < 6 || authManager.isLoading)
                }
            }
            .navigationTitle("Create Account")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}
