package app.tisimai.mektep.data.local

import app.tisimai.mektep.data.models.*
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasteryEngine @Inject constructor(
    private val attemptDao: QuestionAttemptDao,
    private val masteryDao: TopicMasteryDao,
    private val progressDao: ProgressDao,
    private val lessonLoader: LessonLoader
) {
    suspend fun recordAttempts(childId: String, lessonId: String, results: List<AttemptResult>) {
        // Batch insert attempts
        val attempts = results.map { r ->
            QuestionAttempt(
                childId = childId,
                lessonId = lessonId,
                questionIndex = r.questionIndex,
                isCorrect = r.isCorrect,
                responseTimeMs = r.responseTimeMs
            )
        }
        attemptDao.insertAll(attempts)

        // Update mastery via EMA
        val current = masteryDao.get(childId, lessonId)
            ?: TopicMastery(childId = childId, topicId = lessonId)
        var score = current.masteryScore
        for (r in results) {
            val value = if (r.isCorrect) 100.0 else 0.0
            score = score * 0.8 + value * 0.2
        }
        masteryDao.upsert(
            current.copy(
                masteryScore = score,
                totalAttempts = current.totalAttempts + results.size,
                lastUpdatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getMastery(childId: String, lessonId: String): Double {
        return masteryDao.get(childId, lessonId)?.masteryScore ?: 50.0
    }

    suspend fun scheduleReview(childId: String, lessonId: String, masteryScore: Double) {
        val today = LocalDate.now()
        val reviewDate = when {
            masteryScore >= 80 -> today.plusDays(7)
            masteryScore >= 50 -> today.plusDays(3)
            else -> today.plusDays(1)
        }
        val existing = progressDao.getForLesson(childId, lessonId) ?: return
        progressDao.upsert(existing.copy(nextReviewDate = reviewDate.toString()))
    }

    suspend fun selectQuestions(
        childId: String,
        lessonId: String,
        allQuestions: List<QuestionData>,
        maxCount: Int
    ): List<QuestionData> {
        if (childId.isEmpty()) return allQuestions.shuffled().take(maxCount)

        val wrongIndices = try {
            attemptDao.getRecentlyWrongIndices(childId, lessonId).toSet()
        } catch (_: Exception) {
            emptySet()
        }
        if (wrongIndices.isEmpty()) return allQuestions.shuffled().take(maxCount)

        val weak = allQuestions.filterIndexed { i, _ -> i in wrongIndices }.shuffled()
        val other = allQuestions.filterIndexed { i, _ -> i !in wrongIndices }.shuffled()

        // Sandwich pattern: other, weak, other, weak...
        val result = mutableListOf<QuestionData>()
        val weakIter = weak.iterator()
        val otherIter = other.iterator()
        var pickOther = true
        while (result.size < maxCount && (weakIter.hasNext() || otherIter.hasNext())) {
            if (pickOther && otherIter.hasNext()) result.add(otherIter.next())
            else if (weakIter.hasNext()) result.add(weakIter.next())
            else if (otherIter.hasNext()) result.add(otherIter.next())
            pickOther = !pickOther
        }
        return result
    }

    suspend fun getRecommendedLesson(childId: String, gradeLevel: Int): RecommendedLesson? {
        if (childId.isEmpty()) return null
        val today = LocalDate.now().toString()

        // 1. Review due?
        val reviewDue = progressDao.getLessonsDueForReview(childId, today)
        if (reviewDue.isNotEmpty()) return RecommendedLesson(reviewDue.first().lessonId, "review")

        // 2. Next uncompleted in sequence?
        val subjects = lessonLoader.subjectsWithLessonsForGrade(gradeLevel)
        for (subject in subjects) {
            val lessons = lessonLoader.lessonsForSubject(subject.id, gradeLevel)
            for (lesson in lessons) {
                val progress = progressDao.getForLesson(childId, lesson.id)
                if (progress == null || progress.timesCompleted == 0 || progress.bestAccuracy < 60.0) {
                    return RecommendedLesson(lesson.id, "next")
                }
            }
        }

        // 3. Weakest topic?
        val weakest = masteryDao.getWeakest(childId)
        if (weakest != null) return RecommendedLesson(weakest.topicId, "practice")

        return null
    }

    fun isLessonUnlocked(
        subjectLessons: List<Lesson>,
        currentLesson: Lesson,
        allProgress: List<LessonProgress>
    ): Boolean {
        if (currentLesson.sortOrder <= 1) return true
        val previousLesson = subjectLessons.find { it.sortOrder == currentLesson.sortOrder - 1 }
            ?: return true
        val prevProgress = allProgress.find { it.lessonId == previousLesson.id } ?: return false
        return prevProgress.timesCompleted >= 1 && prevProgress.bestAccuracy >= 60.0
    }
}
