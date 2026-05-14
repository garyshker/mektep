import SwiftUI

@MainActor
class ScreenTimeViewModel: ObservableObject {
    @Published var balance: ScreenTimeBalance?
    @Published var isLoading = true

    func load() async {
        isLoading = true
        if let childId = APIClient.shared.childId {
            do {
                balance = try await APIClient.shared.getScreenTimeBalance(childId: childId)
            } catch { }
        }
        isLoading = false
    }
}

struct ScreenTimeView: View {
    @StateObject private var vm = ScreenTimeViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Big timer
                VStack(spacing: 12) {
                    Image(systemName: "timer")
                        .font(.system(size: 56))
                        .foregroundColor(.mektepGreen)

                    let minutes = vm.balance?.balanceMinutes ?? 0
                    let hours = minutes / 60
                    let mins = minutes % 60

                    Text(hours > 0 ? "\(hours)h \(mins)m" : "\(mins)m")
                        .font(.system(size: 48, weight: .bold))
                        .foregroundColor(.mektepGreen)

                    Text("available screen time")
                        .foregroundColor(.secondary)
                }
                .padding(32)
                .frame(maxWidth: .infinity)
                .background(Color.mektepGreen.opacity(0.1))
                .cornerRadius(24)

                // Info cards
                InfoCard(
                    icon: "graduationcap.fill",
                    iconColor: .mektepGreen,
                    title: "Earn More Time",
                    subtitle: "Complete lessons to earn screen time!"
                )

                InfoCard(
                    icon: "info.circle.fill",
                    iconColor: .mektepBlue,
                    title: "How It Works",
                    subtitle: "Each lesson earns XP points. Points convert to minutes of free app usage based on your parent's settings."
                )
            }
            .padding()
        }
        .navigationTitle("Screen Time")
        .task { await vm.load() }
        .overlay { if vm.isLoading { ProgressView() } }
    }
}

struct InfoCard: View {
    let icon: String
    let iconColor: Color
    let title: String
    let subtitle: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(iconColor)
                .font(.title2)
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.headline)
                Text(subtitle).font(.subheadline).foregroundColor(.secondary)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemGray6))
        .cornerRadius(16)
    }
}
