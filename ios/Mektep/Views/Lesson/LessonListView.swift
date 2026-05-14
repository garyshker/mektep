import SwiftUI

@MainActor
class LessonListViewModel: ObservableObject {
    @Published var lessons: [Lesson] = []
    @Published var isLoading = true

    func load(subjectId: String) async {
        isLoading = true
        do {
            lessons = try await APIClient.shared.getLessons(subjectId: subjectId)
        } catch { }
        isLoading = false
    }
}

struct LessonListView: View {
    let subjectId: String
    let subjectName: String
    @StateObject private var vm = LessonListViewModel()
    private let language = APIClient.shared.language

    var body: some View {
        List(vm.lessons) { lesson in
            NavigationLink(destination: LessonRunnerView(lessonId: lesson.id)) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(lesson.title[language] ?? lesson.title["en"] ?? "Lesson")
                        .font(.headline)
                    Text(lesson.description[language] ?? lesson.description["en"] ?? "")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Text("Grade \(lesson.gradeLevel)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.vertical, 4)
            }
        }
        .navigationTitle(subjectName)
        .overlay {
            if vm.isLoading { ProgressView() }
        }
        .task { await vm.load(subjectId: subjectId) }
    }
}
