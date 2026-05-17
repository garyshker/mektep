package app.tisimai.mektep.data.local

import android.content.Context
import app.tisimai.mektep.data.models.Lesson
import app.tisimai.mektep.data.models.LessonFile
import app.tisimai.mektep.data.models.Subject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LessonLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    private var cachedLessons: List<Lesson>? = null
    private var cachedSubjects: List<Subject>? = null

    val subjects: List<Subject>
        get() {
            if (cachedSubjects == null) loadAll()
            return cachedSubjects!!
        }

    val lessons: List<Lesson>
        get() {
            if (cachedLessons == null) loadAll()
            return cachedLessons!!
        }

    fun lessonsForSubject(subjectId: String): List<Lesson> =
        lessons.filter { it.subjectId == subjectId }.sortedBy { it.sortOrder }

    fun lessonsForSubject(subjectId: String, maxGradeLevel: Int): List<Lesson> =
        lessons.filter { it.subjectId == subjectId && it.gradeLevel <= maxGradeLevel }
            .sortedBy { it.sortOrder }

    fun subjectsWithLessonsForGrade(maxGradeLevel: Int): List<Subject> {
        val subjectIdsWithLessons = lessons
            .filter { it.gradeLevel <= maxGradeLevel }
            .map { it.subjectId }
            .toSet()
        return subjects.filter { it.id in subjectIdsWithLessons }
    }

    fun getLesson(lessonId: String): Lesson? =
        lessons.find { it.id == lessonId }

    private fun loadAll() {
        val files = context.assets.list("lessons") ?: emptyArray()
        val allLessons = mutableListOf<Lesson>()
        val subjectMap = mutableMapOf<String, Subject>()

        for (fileName in files) {
            if (!fileName.endsWith(".json")) continue
            try {
                val text = context.assets.open("lessons/$fileName").bufferedReader().readText()
                val lessonFile = json.decodeFromString<LessonFile>(text)

                val subjectId = lessonFile.subject
                if (subjectId !in subjectMap) {
                    subjectMap[subjectId] = subjectFromId(subjectId)
                }

                val lessonId = fileName.removeSuffix(".json")
                allLessons.add(
                    Lesson(
                        id = lessonId,
                        subjectId = subjectId,
                        title = lessonFile.title,
                        description = lessonFile.description,
                        gradeLevel = lessonFile.gradeLevel,
                        sortOrder = lessonFile.sortOrder,
                        questions = lessonFile.questions,
                        fileName = fileName
                    )
                )
            } catch (e: Exception) {
                // Skip malformed files
                e.printStackTrace()
            }
        }

        cachedSubjects = subjectMap.values.toList()
        cachedLessons = allLessons.sortedWith(compareBy({ it.subjectId }, { it.gradeLevel }, { it.sortOrder }))
    }

    private fun subjectFromId(id: String): Subject = when (id) {
        "math" -> Subject(id, mapOf("kk" to "Математика", "ru" to "Математика", "en" to "Math"), "📐", "math")
        "kazakh" -> Subject(id, mapOf("kk" to "Қазақ тілі", "ru" to "Казахский язык", "en" to "Kazakh"), "🇰🇿", "kazakh")
        "english" -> Subject(id, mapOf("kk" to "Ағылшын тілі", "ru" to "Английский язык", "en" to "English"), "🇬🇧", "english")
        "world" -> Subject(id, mapOf("kk" to "Дүниетану", "ru" to "Познание мира", "en" to "World Studies"), "🌍", "world")
        else -> Subject(id, mapOf("en" to id.replaceFirstChar { it.uppercase() }), "📚", id)
    }
}
