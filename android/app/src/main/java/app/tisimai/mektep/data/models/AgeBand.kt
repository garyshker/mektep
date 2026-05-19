package app.tisimai.mektep.data.models

/**
 * Age bands for children 4-8 years old.
 * BOLASHAK (Pre-K, 4-5yo): visual learning, no reading, no failure
 * BALA (Grade 1, 5-7yo): basic literacy/math, gentle encouragement
 * OQYSHY (Grade 2-3, 7-8yo): real exercises, achievements, streaks
 */
enum class AgeBand(
    val gradeRange: IntRange,
    val screenTimeRatio: Double,
    val maxQuestionsPerSession: Int,
    val heartsEnabled: Boolean,
    val heartsCount: Int,
    val dailyLimitDefaultMinutes: Int,
    val showExplanationOnWrong: Boolean,
    val greetingKey: String,
    val labelKey: String,
    val quickGameQuestions: Int,
    val quickGameTimerSec: Int
) {
    BOLASHAK(
        gradeRange = 0..0,
        screenTimeRatio = 2.5,
        maxQuestionsPerSession = 5,
        heartsEnabled = false,
        heartsCount = 0,
        dailyLimitDefaultMinutes = 20,
        showExplanationOnWrong = false,
        greetingKey = "greeting_bolashak",
        labelKey = "band_bolashak",
        quickGameQuestions = 8,
        quickGameTimerSec = 10
    ),
    BALA(
        gradeRange = 1..1,
        screenTimeRatio = 2.0,
        maxQuestionsPerSession = 6,
        heartsEnabled = false,
        heartsCount = 0,
        dailyLimitDefaultMinutes = 30,
        showExplanationOnWrong = false,
        greetingKey = "greeting_bala",
        labelKey = "band_bala",
        quickGameQuestions = 10,
        quickGameTimerSec = 8
    ),
    OQYSHY(
        gradeRange = 2..3,
        screenTimeRatio = 1.5,
        maxQuestionsPerSession = 8,
        heartsEnabled = true,
        heartsCount = 3,
        dailyLimitDefaultMinutes = 45,
        showExplanationOnWrong = false,
        greetingKey = "greeting_oqyshy",
        labelKey = "band_oqyshy",
        quickGameQuestions = 15,
        quickGameTimerSec = 6
    );

    companion object {
        fun fromGradeLevel(grade: Int): AgeBand = when {
            grade <= 0 -> BOLASHAK
            grade <= 1 -> BALA
            else -> OQYSHY
        }
    }
}
