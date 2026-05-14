import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authManager: AuthManager
    @State private var email = ""
    @State private var password = ""
    @State private var showRegister = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()

                Text("Mektep")
                    .font(.system(size: 42, weight: .bold))
                    .foregroundColor(.mektepGreen)

                Text("Learn & Earn Screen Time")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                Spacer().frame(height: 20)

                TextField("Email", text: $email)
                    .textFieldStyle(.roundedBorder)
                    .keyboardType(.emailAddress)
                    .textContentType(.emailAddress)
                    .autocapitalization(.none)

                SecureField("Password", text: $password)
                    .textFieldStyle(.roundedBorder)
                    .textContentType(.password)

                if let error = authManager.error {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                }

                Button {
                    Task { await authManager.login(email: email, password: password) }
                } label: {
                    if authManager.isLoading {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                            .frame(height: 44)
                    } else {
                        Text("Sign In")
                            .frame(maxWidth: .infinity)
                            .frame(height: 44)
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(.mektepGreen)
                .disabled(email.isEmpty || password.isEmpty || authManager.isLoading)

                Button("Don't have an account? Sign Up") {
                    showRegister = true
                }
                .font(.subheadline)

                Spacer()
            }
            .padding(24)
            .sheet(isPresented: $showRegister) {
                RegisterView()
            }
        }
    }
}
