import SwiftUI

struct ParentDashboardView: View {
    @EnvironmentObject var authManager: AuthManager
    @State private var familyName = ""
    @State private var inviteCode: String?

    var body: some View {
        NavigationStack {
            List {
                // Family section
                Section("Family") {
                    TextField("Family Name", text: $familyName)
                    Button("Create Family") {
                        // TODO: Create family via API
                    }
                    .disabled(familyName.isEmpty)

                    if let code = inviteCode {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Invite Code:").font(.caption).foregroundColor(.secondary)
                            Text(code)
                                .font(.title2.bold().monospaced())
                                .foregroundColor(.mektepGreen)
                            Text("Share this with your child").font(.caption).foregroundColor(.secondary)
                        }
                    } else {
                        Button {
                            // TODO: Generate invite via API
                        } label: {
                            Label("Generate Invite Code", systemImage: "person.badge.plus")
                        }
                    }
                }

                // Screen Time Controls
                Section("Screen Time Controls") {
                    HStack {
                        Image(systemName: "timer").foregroundColor(.mektepOrange)
                        VStack(alignment: .leading) {
                            Text("Points per Minute").font(.headline)
                            Text("Default: 10 XP = 1 minute").font(.caption).foregroundColor(.secondary)
                        }
                    }

                    HStack {
                        Image(systemName: "moon.fill").foregroundColor(.purple)
                        VStack(alignment: .leading) {
                            Text("Bedtime Mode").font(.headline)
                            Text("Not configured").font(.caption).foregroundColor(.secondary)
                        }
                    }

                    HStack {
                        Image(systemName: "hand.raised.fill").foregroundColor(.red)
                        VStack(alignment: .leading) {
                            Text("App Blocking").font(.headline)
                            Text("Uses Screen Time API").font(.caption).foregroundColor(.secondary)
                        }
                    }
                }

                // Quick Actions
                Section("Quick Actions") {
                    HStack(spacing: 12) {
                        Button("+15 min") { }
                            .buttonStyle(.bordered)
                        Button("+30 min") { }
                            .buttonStyle(.bordered)
                        Button("Unlock") { }
                            .buttonStyle(.bordered)
                            .tint(.orange)
                    }
                }

                Section {
                    Button("Sign Out", role: .destructive) {
                        authManager.logout()
                    }
                }
            }
            .navigationTitle("Parent Dashboard")
        }
    }
}
