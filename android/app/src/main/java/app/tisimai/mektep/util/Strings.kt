package app.tisimai.mektep.util

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

        // ── Daily Quests ──
        "daily_quests" to mapOf("en" to "Daily Quests", "ru" to "Ежедневные задания", "kk" to "Күнделікті тапсырмалар"),
        "quest_complete_lessons" to mapOf("en" to "Complete %d lesson", "ru" to "Пройди %d урок", "kk" to "%d сабақ аяқта"),
        "quest_earn_xp" to mapOf("en" to "Earn %d XP", "ru" to "Заработай %d XP", "kk" to "%d XP жина"),
        "quest_play_quick_game" to mapOf("en" to "Play Quick Game", "ru" to "Сыграй в Быструю игру", "kk" to "Жылдам ойын ойна"),
        "quest_streak" to mapOf("en" to "Keep your streak!", "ru" to "Сохрани серию!", "kk" to "Серияны сақта!"),
        "quest_score_15" to mapOf("en" to "Score 15+ in Quick Game", "ru" to "Набери 15+ в Быстрой игре", "kk" to "Жылдам ойында 15+ ұпай жина"),
        "claimed" to mapOf("en" to "Claimed!", "ru" to "Получено!", "kk" to "Алынды!"),

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

        // ── Child Launcher ──
        "your_apps" to mapOf("en" to "Your Apps", "ru" to "Твои приложения", "kk" to "Сенің қосымшаларың"),
        "times_up" to mapOf("en" to "Time's Up!", "ru" to "Время вышло!", "kk" to "Уақыт бітті!"),
        "times_up_desc" to mapOf("en" to "Complete a lesson to earn more screen time.", "ru" to "Пройди урок, чтобы заработать ещё экранного времени.", "kk" to "Экран уақытын жинау үшін сабақты орында."),

        // ── Parent Settings ──
        "parent_settings" to mapOf("en" to "Parent Settings", "ru" to "Настройки родителя", "kk" to "Ата-ана баптаулары"),
        "allowed_apps" to mapOf("en" to "Allowed Apps", "ru" to "Разрешённые приложения", "kk" to "Рұқсат етілген қосымшалар"),
        "apps_selected" to mapOf("en" to "%d apps selected", "ru" to "%d приложений выбрано", "kk" to "%d қосымша таңдалды"),
        "daily_limit" to mapOf("en" to "Daily Screen Time Limit", "ru" to "Дневной лимит экранного времени", "kk" to "Күнделікті экран уақыты шектеуі"),
        "minutes" to mapOf("en" to "minutes", "ru" to "минут", "kk" to "минут"),
        "bedtime" to mapOf("en" to "Bedtime", "ru" to "Отбой", "kk" to "Ұйқы уақыты"),
        "bedtime_desc" to mapOf("en" to "Block all apps during bedtime hours", "ru" to "Блокировать все приложения во время отбоя", "kk" to "Ұйқы уақытында барлық қосымшаларды бұғаттау"),
        "not_set" to mapOf("en" to "Not set", "ru" to "Не задано", "kk" to "Орнатылмаған"),
        "screen_time_ratio" to mapOf("en" to "Screen Time Ratio", "ru" to "Коэффициент экранного времени", "kk" to "Экран уақыты коэффициенті"),
        "screen_time_ratio_desc" to mapOf("en" to "1 minute learning = 1.5 minutes screen time", "ru" to "1 минута учёбы = 1.5 минуты экранного времени", "kk" to "1 минут оқу = 1.5 минут экран уақыты"),
        "save" to mapOf("en" to "Save", "ru" to "Сохранить", "kk" to "Сақтау"),

        // ── App Selector ──
        "app_selector_title" to mapOf("en" to "Select Apps", "ru" to "Выбрать приложения", "kk" to "Қосымшаларды таңдау"),
        "app_selector_hint" to mapOf("en" to "Select which apps your child can use. ⏱️ = needs earned time, ✅ = always available.", "ru" to "Выберите, какие приложения может использовать ваш ребёнок. ⏱️ = нужно заработать, ✅ = всегда доступно.", "kk" to "Балаңыз қолдана алатын қосымшаларды таңдаңыз. ⏱️ = жинау керек, ✅ = әрқашан қолжетімді."),

        // ── Pairing ──
        "pairing_parent_title" to mapOf("en" to "Connect Child's Phone", "ru" to "Подключить телефон ребёнка", "kk" to "Баланың телефонын қосу"),
        "pairing_child_title" to mapOf("en" to "Connect to Parent", "ru" to "Подключиться к родителю", "kk" to "Ата-анаға қосылу"),
        "generating_code" to mapOf("en" to "Generating invite code...", "ru" to "Создаём код приглашения...", "kk" to "Шақыру коды жасалуда..."),
        "share_code" to mapOf("en" to "Share this code with your child", "ru" to "Поделитесь этим кодом с ребёнком", "kk" to "Бұл кодты балаңызбен бөлісіңіз"),
        "code_expires" to mapOf("en" to "Code expires in 24 hours", "ru" to "Код действует 24 часа", "kk" to "Код 24 сағат жарамды"),
        "waiting_child" to mapOf("en" to "Waiting for child to connect", "ru" to "Ожидание подключения ребёнка", "kk" to "Баланың қосылуын күтуде"),
        "enter_invite_code" to mapOf("en" to "Enter Invite Code", "ru" to "Введите код приглашения", "kk" to "Шақыру кодын енгізіңіз"),
        "ask_parent_code" to mapOf("en" to "Ask your parent for the 6-letter code from their Mektep app", "ru" to "Спросите у родителя 6-значный код из их приложения Mektep", "kk" to "Ата-анаңыздан Mektep қосымшасындағы 6 таңбалы кодты сұраңыз"),
        "invite_code" to mapOf("en" to "Invite Code", "ru" to "Код приглашения", "kk" to "Шақыру коды"),
        "join_family" to mapOf("en" to "Join Family", "ru" to "Присоединиться", "kk" to "Қосылу"),

        // ── Parent Remote Dashboard ──
        "parent_dashboard" to mapOf("en" to "Parent Dashboard", "ru" to "Панель родителя", "kk" to "Ата-ана тақтасы"),
        "connected_children" to mapOf("en" to "Connected Children", "ru" to "Подключённые дети", "kk" to "Қосылған балалар"),
        "no_children_yet" to mapOf("en" to "No children connected yet", "ru" to "Пока нет подключённых детей", "kk" to "Әлі қосылған бала жоқ"),
        "share_invite" to mapOf("en" to "Share the invite code above with your child", "ru" to "Поделитесь кодом с ребёнком", "kk" to "Жоғарыдағы кодты балаңызбен бөлісіңіз"),
        "child_mode_active" to mapOf("en" to "Child mode active", "ru" to "Детский режим активен", "kk" to "Балалар режимі белсенді"),
        "child_mode_inactive" to mapOf("en" to "Online", "ru" to "В сети", "kk" to "Желіде"),
        "remaining" to mapOf("en" to "remaining", "ru" to "осталось", "kk" to "қалды"),
        "bonus_granted" to mapOf("en" to "Bonus time sent!", "ru" to "Бонусное время отправлено!", "kk" to "Бонус уақыт жіберілді!"),
    )
}

/** Shorthand for getting a translated string */
fun tr(key: String, lang: String): String = Strings.get(key, lang)
fun tr(key: String, lang: String, vararg args: Any): String = Strings.get(key, lang).format(*args)
