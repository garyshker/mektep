package app.tisimai.mektep.data.models

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
    ),
    ZERDE(
        gradeRange = 4..6,
        screenTimeRatio = 1.0,
        maxQuestionsPerSession = 10,
        heartsEnabled = true,
        heartsCount = 3,
        dailyLimitDefaultMinutes = 60,
        showExplanationOnWrong = true,
        greetingKey = "greeting_zerde",
        labelKey = "band_zerde",
        quickGameQuestions = 20,
        quickGameTimerSec = 5
    );

    companion object {
        fun fromGradeLevel(grade: Int): AgeBand = when {
            grade <= 1 -> BALA
            grade <= 3 -> OQYSHY
            else -> ZERDE
        }
    }
}
