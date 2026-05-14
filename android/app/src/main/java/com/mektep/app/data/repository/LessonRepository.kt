package com.mektep.app.data.repository

import com.mektep.app.data.api.MektepApi
import com.mektep.app.data.models.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LessonRepository @Inject constructor(
    private val api: MektepApi
) {
    suspend fun getSubjects(): List<Subject> = api.getSubjects()

    suspend fun getLessons(subjectId: String, gradeLevel: Int? = null): List<Lesson> =
        api.getLessons(subjectId, gradeLevel)

    suspend fun getLesson(lessonId: String): Lesson = api.getLesson(lessonId)

    suspend fun startAttempt(lessonId: String, childId: String): LessonAttempt =
        api.startAttempt(lessonId, StartAttemptRequest(childId))

    suspend fun submitAnswer(attemptId: String, questionId: String, answer: String, timeMs: Int): SubmitAnswerResponse =
        api.submitAnswer(attemptId, SubmitAnswerRequest(questionId, answer, timeMs))

    suspend fun completeAttempt(attemptId: String): CompleteResponse =
        api.completeAttempt(attemptId)

    suspend fun getDashboard(childId: String): DashboardResponse =
        api.getDashboard(childId)
}
