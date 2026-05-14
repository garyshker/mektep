import SwiftUI

@MainActor
class LessonRunnerViewModel: ObservableObject {
    @Published var isLoading = true
    @Published var questions: [Question] = []
    @Published var currentIndex = 0
    @Published var hearts = 3
    @Published var selectedAnswer = ""
    @Published var feedbackShown = false
    @Published var lastCorrect = false
    @Published var isCompleted = false
    @Published var score = 0
    @Published var xpEarned = 0
    @Published var starsEarned = 0
    @Published var accuracyPct = 0.0

    private var attemptId = ""
    private let api = APIClient.shared

    var currentQuestion: Question? {
        guard currentIndex < questions.count else { return nil }
        return questions[currentIndex]
    }

    var totalQuestions: Int { questions.count }

    func load(lessonId: String) async {
        isLoading = true
        do {
            let lesson = try await api.getLesson(id: lessonId)
            questions = lesson.questions

            if let childId = api.childId {
                let attempt = try await api.startAttempt(lessonId: lessonId, childId: childId)
                attemptId = attempt.id
            }
        } catch { }
        isLoading = false
    }

    func selectAnswer(_ answer: String) {
        selectedAnswer = answer
    }

    func submit() async {
        guard let q = currentQuestion else { return }
        let timeMs = 5000 // simplified

        var isCorrect = false
        do {
            if !attemptId.isEmpty {
                let resp = try await api.submitAnswer(
                    attemptId: attemptId,
                    request: SubmitAnswerRequest(questionId: q.id, givenAnswer: selectedAnswer, timeSpentMs: timeMs)
                )
                isCorrect = resp.isCorrect
            } else {
                isCorrect = checkLocally(q)
            }
        } catch {
            isCorrect = checkLocally(q)
        }

        if isCorrect { score += 1 } else { hearts = max(0, hearts - 1) }
        lastCorrect = isCorrect
        feedbackShown = true
    }

    func next() {
        feedbackShown = false
        selectedAnswer = ""
        let nextIdx = currentIndex + 1

        if nextIdx >= totalQuestions || hearts <= 0 {
            complete()
            return
        }
        currentIndex = nextIdx
    }

    private func complete() {
        Task {
            do {
                if !attemptId.isEmpty {
                    let result = try await api.completeAttempt(attemptId: attemptId)
                    score = result.score
                    xpEarned = result.xpEarned
                    starsEarned = result.starsEarned
                    accuracyPct = result.accuracyPct
                    isCompleted = true
                    return
                }
            } catch { }

            // Local fallback
            let acc = totalQuestions > 0 ? Double(score) / Double(totalQuestions) * 100 : 0
            starsEarned = acc >= 95 ? 3 : (acc >= 80 ? 2 : 1)
            xpEarned = score * 5 + (starsEarned == 3 ? 20 : starsEarned == 2 ? 10 : 0)
            accuracyPct = acc
            isCompleted = true
        }
    }

    private func checkLocally(_ q: Question) -> Bool {
        guard let correct = q.correctAnswer?.value else { return false }
        if let s = correct as? String {
            return s.lowercased() == selectedAnswer.lowercased().trimmingCharacters(in: .whitespaces)
        }
        if let n = correct as? Int {
            return "\(n)" == selectedAnswer.trimmingCharacters(in: .whitespaces)
        }
        return false
    }
}

struct LessonRunnerView: View {
    let lessonId: String
    @StateObject private var vm = LessonRunnerViewModel()
    @Environment(\.dismiss) var dismiss
    private let language = APIClient.shared.language

    var body: some View {
        Group {
            if vm.isLoading {
                ProgressView()
            } else if vm.isCompleted {
                completionView
            } else if let q = vm.currentQuestion {
                questionView(q)
            }
        }
        .navigationBarBackButtonHidden(true)
        .task { await vm.load(lessonId: lessonId) }
    }

    @ViewBuilder
    private func questionView(_ q: Question) -> some View {
        VStack(spacing: 16) {
            // Top bar
            HStack {
                Button { dismiss() } label: {
                    Image(systemName: "xmark").font(.title3)
                }
                ProgressView(value: Double(vm.currentIndex + 1), total: Double(vm.totalQuestions))
                    .tint(.mektepGreen)
                HStack(spacing: 2) {
                    ForEach(0..<vm.hearts, id: \.self) { _ in
                        Image(systemName: "heart.fill").foregroundColor(.mektepRed).font(.caption)
                    }
                }
            }

            Text("Question \(vm.currentIndex + 1) of \(vm.totalQuestions)")
                .font(.caption).foregroundColor(.secondary)

            Text(q.prompt[language] ?? q.prompt["en"] ?? "")
                .font(.title2.bold())
                .multilineTextAlignment(.center)
                .padding(.vertical)

            // Question type
            ScrollView {
                switch q.type {
                case "mc", "word":
                    mcView(q)
                case "type":
                    typeView()
                case "tap":
                    tapView(q)
                case "match":
                    matchView(q)
                default:
                    mcView(q)
                }
            }

            // Feedback
            if vm.feedbackShown {
                HStack {
                    Image(systemName: vm.lastCorrect ? "checkmark.circle.fill" : "xmark.circle.fill")
                    Text(vm.lastCorrect ? "Correct!" : "Incorrect")
                        .bold()
                }
                .foregroundColor(vm.lastCorrect ? .mektepGreen : .mektepRed)
                .padding()
                .frame(maxWidth: .infinity)
                .background((vm.lastCorrect ? Color.green : Color.red).opacity(0.1))
                .cornerRadius(12)
            }

            // Button
            Button {
                if vm.feedbackShown {
                    vm.next()
                } else {
                    Task { await vm.submit() }
                }
            } label: {
                Text(vm.feedbackShown ? "Continue" : "Check")
                    .frame(maxWidth: .infinity).frame(height: 44).bold()
            }
            .buttonStyle(.borderedProminent)
            .tint(.mektepGreen)
            .disabled(vm.selectedAnswer.isEmpty && !vm.feedbackShown)
        }
        .padding()
    }

    @ViewBuilder
    private func mcView(_ q: Question) -> some View {
        if let opts = q.options?.value as? [Any] {
            VStack(spacing: 8) {
                ForEach(Array(opts.enumerated()), id: \.offset) { idx, opt in
                    let text = optionText(opt)
                    let selected = vm.selectedAnswer == "\(idx)"

                    Button {
                        if !vm.feedbackShown { vm.selectAnswer("\(idx)") }
                    } label: {
                        Text(text)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(selected ? Color.mektepGreen.opacity(0.15) : Color(.systemGray6))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(selected ? Color.mektepGreen : Color.clear, lineWidth: 2)
                            )
                            .cornerRadius(12)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    @ViewBuilder
    private func typeView() -> some View {
        TextField("Your answer", text: Binding(
            get: { vm.selectedAnswer },
            set: { vm.selectAnswer($0) }
        ))
        .textFieldStyle(.roundedBorder)
        .font(.title2)
        .multilineTextAlignment(.center)
        .disabled(vm.feedbackShown)
    }

    @ViewBuilder
    private func tapView(_ q: Question) -> some View {
        if let opts = q.options?.value as? [Any] {
            let selected = Set(vm.selectedAnswer.split(separator: ",").map(String.init))
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 100))], spacing: 8) {
                ForEach(Array(opts.enumerated()), id: \.offset) { idx, opt in
                    let text = optionText(opt)
                    let isSelected = selected.contains("\(idx)")

                    Button {
                        guard !vm.feedbackShown else { return }
                        var s = selected
                        if isSelected { s.remove("\(idx)") } else { s.insert("\(idx)") }
                        vm.selectAnswer(s.joined(separator: ","))
                    } label: {
                        Text(text)
                            .padding(.horizontal, 16).padding(.vertical, 10)
                            .background(isSelected ? Color.mektepGreen.opacity(0.2) : Color(.systemGray6))
                            .cornerRadius(20)
                            .overlay(
                                RoundedRectangle(cornerRadius: 20)
                                    .stroke(isSelected ? Color.mektepGreen : Color.clear, lineWidth: 2)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    @ViewBuilder
    private func matchView(_ q: Question) -> some View {
        if let pairs = q.pairs?.value as? [[String: Any]] {
            VStack(spacing: 8) {
                ForEach(Array(pairs.enumerated()), id: \.offset) { _, pair in
                    HStack {
                        Text(pair["left"] as? String ?? "")
                            .frame(maxWidth: .infinity)
                            .padding(10)
                            .background(Color.mektepGreen.opacity(0.1))
                            .cornerRadius(8)
                        Image(systemName: "arrow.right")
                        Text(pair["right"] as? String ?? "")
                            .frame(maxWidth: .infinity)
                            .padding(10)
                            .background(Color.mektepBlue.opacity(0.1))
                            .cornerRadius(8)
                    }
                }
            }
            .onAppear { vm.selectAnswer("matched") }
        }
    }

    private var completionView: some View {
        VStack(spacing: 20) {
            Spacer()
            Text("🎉").font(.system(size: 64))
            Text("Lesson Complete!").font(.title.bold())

            HStack(spacing: 4) {
                ForEach(0..<3, id: \.self) { i in
                    Image(systemName: "star.fill")
                        .font(.title)
                        .foregroundColor(i < vm.starsEarned ? .yellow : .gray.opacity(0.3))
                }
            }

            HStack(spacing: 32) {
                VStack { Text("\(vm.xpEarned)").font(.title.bold()).foregroundColor(.mektepGreen); Text("XP") }
                VStack { Text("\(Int(vm.accuracyPct))%").font(.title.bold()); Text("Accuracy") }
                VStack { Text("\(vm.score)/\(vm.totalQuestions)").font(.title.bold()); Text("Correct") }
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(16)

            Spacer()

            Button { dismiss() } label: {
                Text("Continue")
                    .frame(maxWidth: .infinity).frame(height: 44).bold()
            }
            .buttonStyle(.borderedProminent)
            .tint(.mektepGreen)
        }
        .padding(32)
    }

    private func optionText(_ opt: Any) -> String {
        if let arr = opt as? [Any], let first = arr.first as? [String: Any] {
            return (first[language] ?? first["en"] ?? "") as? String ?? ""
        }
        if let dict = opt as? [String: Any] {
            return (dict[language] ?? dict["en"] ?? "") as? String ?? ""
        }
        return "\(opt)"
    }
}
