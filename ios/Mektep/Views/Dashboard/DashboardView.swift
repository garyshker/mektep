import SwiftUI

@MainActor
class DashboardViewModel: ObservableObject {
    @Published var subjects: [Subject] = []
    @Published var profile: ChildProfile?
    @Published var progress: [SubjectProgress] = []
    @Published var balance: ScreenTimeBalance?
    @Published var isLoading = true

    private let api = APIClient.shared

    func load() async {
        isLoading = true
        do {
            subjects = try await api.getSubjects()
            if let childId = api.childId {
                let dashboard = try await api.getDashboard(childId: childId)
                profile = dashboard.profile
                progress = dashboard.progress
                balance = try await api.getScreenTimeBalance(childId: childId)
            }
        } catch { }
        isLoading = false
    }
}

struct DashboardView: View {
    @StateObject private var vm = DashboardViewModel()
    private let language = APIClient.shared.language

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Stats
                    if let profile = vm.profile {
                        HStack(spacing: 24) {
                            StatBubble(icon: "flame.fill", value: "\(profile.currentStreak)", label: "Streak", color: .mektepOrange)
                            StatBubble(icon: "star.fill", value: "\(profile.xpTotal)", label: "XP", color: .mektepGreen)
                            StatBubble(icon: "trophy.fill", value: "Lv \(profile.xpTotal / 100 + 1)", label: "Level", color: .mektepBlue)
                        }
                        .padding(.vertical, 8)
                    }

                    // Screen Time Card
                    NavigationLink(destination: ScreenTimeView()) {
                        HStack {
                            Image(systemName: "timer")
                                .font(.title)
                                .foregroundColor(.mektepGreen)
                            VStack(alignment: .leading) {
                                Text("Screen Time").font(.headline)
                                Text("\(vm.balance?.balanceMinutes ?? 0) minutes available")
                                    .font(.subheadline).foregroundColor(.secondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right").foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color.mektepGreen.opacity(0.1))
                        .cornerRadius(16)
                    }
                    .buttonStyle(.plain)

                    // Subjects
                    Text("Subjects")
                        .font(.title2.bold())
                        .frame(maxWidth: .infinity, alignment: .leading)

                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        ForEach(vm.subjects) { subject in
                            NavigationLink(destination: LessonListView(subjectId: subject.id, subjectName: subject.name[language] ?? subject.name["en"] ?? "")) {
                                SubjectCard(subject: subject, progress: vm.progress.first { $0.subjectId == subject.id }, language: language)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Mektep")
            .task { await vm.load() }
            .refreshable { await vm.load() }
        }
    }
}

struct StatBubble: View {
    let icon: String
    let value: String
    let label: String
    let color: Color

    var body: some View {
        VStack(spacing: 4) {
            ZStack {
                Circle().fill(color.opacity(0.15)).frame(width: 48, height: 48)
                Image(systemName: icon).foregroundColor(color)
            }
            Text(value).font(.headline)
            Text(label).font(.caption).foregroundColor(.secondary)
        }
    }
}

struct SubjectCard: View {
    let subject: Subject
    let progress: SubjectProgress?
    let language: String

    private var color: Color {
        if subject.name["en"]?.contains("Math") == true { return .mathColor }
        if subject.name["en"]?.contains("Kazakh") == true { return .kazakhColor }
        if subject.name["en"]?.contains("English") == true { return .englishColor }
        if subject.name["en"]?.contains("World") == true { return .worldColor }
        return .mektepGreen
    }

    private var emoji: String {
        if subject.name["en"]?.contains("Math") == true { return "📐" }
        if subject.name["en"]?.contains("Kazakh") == true { return "🇰🇿" }
        if subject.name["en"]?.contains("English") == true { return "🇬🇧" }
        if subject.name["en"]?.contains("World") == true { return "🌍" }
        return "📚"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(emoji).font(.title)
            Spacer()
            Text(subject.name[language] ?? subject.name["en"] ?? "Subject")
                .font(.headline)
            if let p = progress, p.totalLessons > 0 {
                ProgressView(value: Double(p.completedLessons), total: Double(p.totalLessons))
                    .tint(color)
                Text("\(p.completedLessons)/\(p.totalLessons) lessons")
                    .font(.caption).foregroundColor(.secondary)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, minHeight: 140, alignment: .leading)
        .background(color.opacity(0.1))
        .cornerRadius(16)
    }
}
