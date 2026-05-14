package com.mektep.app.data.api

import com.mektep.app.data.models.*
import retrofit2.http.*

interface MektepApi {

    // Auth
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponse

    // Family
    @POST("families")
    suspend fun createFamily(@Body request: CreateFamilyRequest): Family

    @GET("families/{familyId}")
    suspend fun getFamily(@Path("familyId") familyId: String): Family

    @POST("families/{familyId}/invite")
    suspend fun generateInvite(@Path("familyId") familyId: String): InviteCode

    @POST("families/join")
    suspend fun joinFamily(@Body request: JoinFamilyRequest): Family

    @GET("families/{familyId}/members")
    suspend fun listMembers(@Path("familyId") familyId: String): List<FamilyMember>

    // Subjects & Lessons
    @GET("subjects")
    suspend fun getSubjects(): List<Subject>

    @GET("subjects/{subjectId}/lessons")
    suspend fun getLessons(
        @Path("subjectId") subjectId: String,
        @Query("grade") gradeLevel: Int? = null
    ): List<Lesson>

    @GET("lessons/{lessonId}")
    suspend fun getLesson(@Path("lessonId") lessonId: String): Lesson

    // Lesson Attempts
    @POST("lessons/{lessonId}/start")
    suspend fun startAttempt(
        @Path("lessonId") lessonId: String,
        @Body request: StartAttemptRequest
    ): LessonAttempt

    @POST("lesson-attempts/{attemptId}/answer")
    suspend fun submitAnswer(
        @Path("attemptId") attemptId: String,
        @Body request: SubmitAnswerRequest
    ): SubmitAnswerResponse

    @POST("lesson-attempts/{attemptId}/complete")
    suspend fun completeAttempt(@Path("attemptId") attemptId: String): CompleteResponse

    // Dashboard
    @GET("children/{childId}/dashboard")
    suspend fun getDashboard(@Path("childId") childId: String): DashboardResponse

    @GET("children/{childId}/progress")
    suspend fun getProgress(@Path("childId") childId: String): List<SubjectProgress>

    // Screen Time
    @GET("children/{childId}/screen-time/balance")
    suspend fun getScreenTimeBalance(@Path("childId") childId: String): ScreenTimeBalance

    @POST("children/{childId}/screen-time/spend")
    suspend fun spendScreenTime(
        @Path("childId") childId: String,
        @Body request: SpendRequest
    ): ScreenTimeBalance

    @POST("children/{childId}/screen-time/bonus")
    suspend fun grantBonus(
        @Path("childId") childId: String,
        @Body request: BonusRequest
    ): ScreenTimeBalance

    @GET("children/{childId}/config")
    suspend fun getScreenTimeConfig(@Path("childId") childId: String): ScreenTimeConfig

    @PUT("children/{childId}/config")
    suspend fun updateScreenTimeConfig(
        @Path("childId") childId: String,
        @Body config: ScreenTimeConfig
    ): ScreenTimeConfig
}
