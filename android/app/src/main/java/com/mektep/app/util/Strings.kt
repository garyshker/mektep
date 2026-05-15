package com.mektep.app.util

/**
 * Centralized trilingual string lookup.
 * All user-visible text goes here so we never have hardcoded English-only strings.
 */
object Strings {

    fun get(key: String, lang: String): String = translations[key]?.get(lang) ?: translations[key]?.get("en") ?: key

    private val translations = mapOf(
        // ── Login screen ──
        "app_tagline" to mapOf("en" to "Learn & Earn Screen Time", "ru" to "Учись и зарабатывай экранное время", "kk" to "Үйреніп, экран уақытын жина"),
        "sign_in_google" to mapOf("en" to "Sign in with Google", "ru" to "Войти через Google", "kk" to "Google арқылы кіру"),
        "continue_without_account" to mapOf("en" to "Continue without account", "ru" to "Продолжить без аккаунта", "kk" to "Тіркеусіз жалғастыру"),
        "login_motivation" to mapOf("en" to "Complete lessons to earn screen time.\nThe more you learn, the more you play!", "ru" to "Выполняй уроки и зарабатывай экранное время.\nБольше учишься — больше играешь!", "kk" to "Сабақтарды орындап, экран уақытын жина.\nКөбірек үйренсең, көбірек ойнайсың!"),

        // ── Dashboard ──
        "screen_time" to mapOf("en" to "Screen Time", "ru" to "Экранное время", "kk" to "Экран уақыты"),
        "minutes_available" to mapOf("en" to "minutes available", "ru" to "минут доступно", "kk" to "минут қолжетімді"),
        "subjects" to mapOf("en" to "Subjects", "ru" to "Предметы", "kk" to "Пәндер"),
        "lessons" to mapOf("en" to "lessons", "ru" to "уроков", "kk" to "сабақ"),
        "streak" to mapOf("en" to "Streak", "ru" to "Серия", "kk" to "Серия"),
        "level" to mapOf("en" to "Level", "ru" to "Уровень", "kk" to "Деңгей"),
        "start_child_mode" to mapOf("en" to "Start Child Mode", "ru" to "Детский режим", "kk" to "Балалар режимі"),

        // ── Quick Game ──
        "quick_game" to mapOf("en" to "Quick Game", "ru" to "Быстрая игра", "kk" to "Жылдам ойын"),
        "quick_game_desc" to mapOf("en" to "20 math problems, 5 sec each!", "ru" to "20 задач, 5 сек на каждую!", "kk" to "20 есеп, әрқайсысына 5 секунд!"),
        "score" to mapOf("en" to "Score", "ru" to "Счёт", "kk" to "Ұпай"),
        "correct_answers" to mapOf("en" to "correct answers", "ru" to "правильных ответов", "kk" to "дұрыс жауап"),
        "play_again" to mapOf("en" to "Play Again", "ru" to "Играть ещё", "kk" to "Қайта ойнау"),
        "back_to_dashboard" to mapOf("en" to "Back to Dashboard", "ru" to "На главную", "kk" to "Басты бетке"),
        "excellent" to mapOf("en" to "Excellent!", "ru" to "Отлично!", "kk" to "Тамаша!"),
        "great" to mapOf("en" to "Great!", "ru" to "Здорово!", "kk" to "Керемет!"),
        "good" to mapOf("en" to "Good!", "ru" to "Хорошо!", "kk" to "Жақсы!"),
        "keep_practicing" to mapOf("en" to "Keep practicing!", "ru" to "Продолжай тренироваться!", "kk" to "Жаттығуды жалғастыр!"),

        // ── Lesson runner ──
        "question_n_of_m" to mapOf("en" to "Question %d of %d", "ru" to "Вопрос %d из %d", "kk" to "%d/%d сұрақ"),
        "check" to mapOf("en" to "Check", "ru" to "Проверить", "kk" to "Тексеру"),
        "continue_btn" to mapOf("en" to "Continue", "ru" to "Продолжить", "kk" to "Жалғастыру"),
        "correct" to mapOf("en" to "Correct!", "ru" to "Правильно!", "kk" to "Дұрыс!"),
        "incorrect" to mapOf("en" to "Incorrect", "ru" to "Неправильно", "kk" to "Қате"),
        "lesson_complete" to mapOf("en" to "Lesson Complete!", "ru" to "Урок завершён!", "kk" to "Сабақ аяқталды!"),
        "xp_earned" to mapOf("en" to "XP Earned", "ru" to "XP получено", "kk" to "XP жиналды"),
        "accuracy" to mapOf("en" to "Accuracy", "ru" to "Точность", "kk" to "Дәлдік"),
        "screen_time_earned" to mapOf("en" to "screen time earned", "ru" to "экранное время", "kk" to "экран уақыты"),
        "your_answer" to mapOf("en" to "Your answer", "ru" to "Ваш ответ", "kk" to "Сіздің жауабыңыз"),
        "tap_correct" to mapOf("en" to "Tap all correct answers:", "ru" to "Выберите все правильные:", "kk" to "Барлық дұрыс жауапты таңдаңыз:"),
        "match_pairs" to mapOf("en" to "Match the pairs:", "ru" to "Сопоставьте пары:", "kk" to "Жұптарды сәйкестендіріңіз:"),

        // ── Screen Time ──
        "available_screen_time" to mapOf("en" to "available screen time", "ru" to "доступное экранное время", "kk" to "қолжетімді экран уақыты"),
        "earn_more_time" to mapOf("en" to "Earn More Time", "ru" to "Заработай ещё", "kk" to "Көбірек уақыт жина"),
        "earn_more_desc" to mapOf("en" to "Complete lessons to earn screen time!", "ru" to "Выполняй уроки, чтобы заработать экранное время!", "kk" to "Экран уақытын жинау үшін сабақтарды орында!"),
        "how_it_works" to mapOf("en" to "How It Works", "ru" to "Как это работает", "kk" to "Қалай жұмыс істейді"),
        "how_it_works_desc" to mapOf("en" to "Each correct answer earns 5 XP. 10 XP = 1 minute of screen time. Bonus XP for high accuracy!", "ru" to "Каждый правильный ответ — 5 XP. 10 XP = 1 минута. Бонус за точность!", "kk" to "Әр дұрыс жауап — 5 XP. 10 XP = 1 минут. Жоғары дәлдік үшін бонус!"),

        // ── Setup ──
        "setup_title" to mapOf("en" to "How will you use Mektep?", "ru" to "Как вы будете использовать Mektep?", "kk" to "Mektep-ті қалай қолданасыз?"),
        "setup_change_later" to mapOf("en" to "You can change this later in settings", "ru" to "Вы можете изменить это позже в настройках", "kk" to "Мұны кейін баптауларда өзгерте аласыз"),
        "setup_solo" to mapOf("en" to "I'm learning on my own", "ru" to "Я учусь сам", "kk" to "Мен өзім үйренемін"),
        "setup_solo_desc" to mapOf("en" to "Just lessons and earning screen time. No parental controls.", "ru" to "Только уроки и экранное время. Без родительского контроля.", "kk" to "Тек сабақтар мен экран уақыты. Ата-ана бақылаусыз."),
        "setup_same_device" to mapOf("en" to "Child Mode on this phone", "ru" to "Детский режим на этом телефоне", "kk" to "Осы телефонда балалар режимі"),
        "setup_same_device_desc" to mapOf("en" to "I'm a parent. My child uses this phone. Lock it down when they play.", "ru" to "Я родитель. Мой ребёнок пользуется этим телефоном.", "kk" to "Мен ата-анамын. Балам осы телефонды пайдаланады."),
        "setup_remote_parent" to mapOf("en" to "I'm a parent (remote control)", "ru" to "Я родитель (удалённый контроль)", "kk" to "Мен ата-анамын (қашықтан басқару)"),
        "setup_remote_parent_desc" to mapOf("en" to "My child has their own phone. I want to control their screen time from here.", "ru" to "У ребёнка свой телефон. Хочу контролировать экранное время отсюда.", "kk" to "Баламның жеке телефоны бар. Экран уақытын осы жерден басқарғым келеді."),
        "setup_remote_child" to mapOf("en" to "I'm a child (connect to parent)", "ru" to "Я ребёнок (подключиться к родителю)", "kk" to "Мен баламын (ата-анаға қосылу)"),
        "setup_remote_child_desc" to mapOf("en" to "My parent has the Mektep app. I want to enter their invite code.", "ru" to "У моего родителя есть Mektep. Хочу ввести код приглашения.", "kk" to "Ата-анамда Mektep бар. Шақыру кодын енгізгім келеді."),

        // ── PIN ──
        "create_pin" to mapOf("en" to "Create a PIN", "ru" to "Создайте PIN-код", "kk" to "PIN-код жасаңыз"),
        "create_pin_desc" to mapOf("en" to "This PIN locks Child Mode. Only you should know it.", "ru" to "Этот PIN блокирует детский режим. Только вы должны его знать.", "kk" to "Бұл PIN балалар режимін құлыптайды. Тек сіз білуіңіз керек."),
        "confirm_pin" to mapOf("en" to "Confirm your PIN", "ru" to "Подтвердите PIN-код", "kk" to "PIN-кодты растаңыз"),
        "confirm_pin_desc" to mapOf("en" to "Enter the same PIN again", "ru" to "Введите тот же PIN ещё раз", "kk" to "Сол PIN-кодты қайта енгізіңіз"),
        "wrong_pin" to mapOf("en" to "Wrong PIN", "ru" to "Неверный PIN", "kk" to "Қате PIN"),
        "pins_dont_match" to mapOf("en" to "PINs don't match. Try again.", "ru" to "PIN-коды не совпадают. Попробуйте снова.", "kk" to "PIN-кодтар сәйкес келмейді. Қайталаңыз."),
        "enter_pin_activate" to mapOf("en" to "Enter PIN to start Child Mode", "ru" to "Введите PIN для детского режима", "kk" to "Балалар режимі үшін PIN енгізіңіз"),
        "enter_pin_deactivate" to mapOf("en" to "Enter PIN to exit Child Mode", "ru" to "Введите PIN для выхода", "kk" to "Шығу үшін PIN енгізіңіз"),
    )
}

/** Shorthand for getting a translated string */
fun tr(key: String, lang: String): String = Strings.get(key, lang)
fun tr(key: String, lang: String, vararg args: Any): String = Strings.get(key, lang).format(*args)
