// app.jsx — iMektep main application
// Loaded 3rd by the sequential loader in index.html, after:
//   tweaks-panel.jsx  → sets window.useTweaks, window.TweaksPanel, etc.
//   lesson-runner.jsx → sets window.LessonRunner, window.pickLang, etc.

const { useState, useEffect } = React;

// Capture shared dependencies from window at module-load time.
// Because the sequential loader ran the above two files first, these are
// already set by the time this file executes.
const {
  LessonRunner,
  pickLang,
} = window;

const {
  useTweaks,
  TweaksPanel, TweakSection, TweakRow,
  TweakToggle, TweakRadio, TweakSelect, TweakButton,
} = window;

// ─── Tweaks defaults ───────────────────────────────────────────────

const TWEAK_DEFAULTS = {
  mascot: true,
  density: "comfortable",
  showCallouts: true,
  language: "kk",
};

// ─── Subject icons ─────────────────────────────────────────────────

const IconMath = (p) => (
  <svg viewBox="0 0 24 24" fill="none" {...p}>
    <rect x="3" y="3" width="18" height="18" rx="5" stroke="currentColor" strokeWidth="2"/>
    <path d="M7 8h10M9 12h6M7 16h4M14 16l3 3M17 16l-3 3" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
  </svg>
);
const IconKaz = (p) => (
  <svg viewBox="0 0 24 24" fill="none" {...p}>
    <path d="M4 5.5C7 4 10 4 12 5.5C14 4 17 4 20 5.5V19c-3-1.5-6-1.5-8 0c-2-1.5-5-1.5-8 0V5.5Z" stroke="currentColor" strokeWidth="2" strokeLinejoin="round"/>
    <path d="M12 5.5V19" stroke="currentColor" strokeWidth="2"/>
  </svg>
);
const IconWorld = (p) => (
  <svg viewBox="0 0 24 24" fill="none" {...p}>
    <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2"/>
    <path d="M3 12h18M12 3c2.5 3 2.5 15 0 18M12 3c-2.5 3-2.5 15 0 18" stroke="currentColor" strokeWidth="2"/>
  </svg>
);
const IconEng = (p) => (
  <svg viewBox="0 0 24 24" fill="none" {...p}>
    <path d="M4 18l4-10 4 10M5.5 14h5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M14 12c1.5-2 4-2 5 0c1 2-2 3-2.5 5h3" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M14 18h6" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
  </svg>
);
const Sparkle = (p) => (
  <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
    <path d="M12 2l1.6 4.7L18 8l-4.4 1.3L12 14l-1.6-4.7L6 8l4.4-1.3L12 2zM18 14l.9 2.6L21 17.5l-2.1.9L18 21l-.9-2.6L15 17.5l2.1-.9L18 14z"/>
  </svg>
);
const Star = ({ on }) => (
  <svg className={"s " + (on ? "" : "off")} viewBox="0 0 24 24" fill="currentColor">
    <path d="M12 17.3l-6.2 3.7 1.6-7L2 9.2l7.1-.6L12 2l2.9 6.6 7.1.6-5.4 4.8 1.6 7z"/>
  </svg>
);

// ─── UI strings (3 languages) ──────────────────────────────────────

const L = {
  kk: {
    welcome: "Сәлем, Али!",
    welcomeSub: "Бүгін не үйренеміз?",
    eyebrowPick: "ПӘНДІ ТАҢДА",
    title: "Бастауыш сыныптарға арналған сабақтар",
    continueEyebrow: "Жалғастыр",
    continueTitle: "Қосу · 100 ішінде · 1-сабақ",
    continueSub: "Сен бұл сабаққа кеше уақыт бөлдің. Енді 5 минут қалды.",
    continueBtn: "Сабақты жалғастыру",
    quests: "Бүгінгі тапсырмалар",
    questsPill: "3 / 4",
    q1: "Бір сабақты аяқта",   q1m: "≈ 8 мин",
    q2: "20 карточкамен жатта", q2m: "Қазақ тілі",
    q3: "Бір викторинаны өт",   q3m: "Математика",
    q4: "Достарыңмен ойна",     q4m: "Жаңа!",
    eyebrowAll: "БАРЛЫҚ ПӘНДЕР",
    all: "Барлық пәндер",
    seeAll: "Барлығын көру",
    soon: "ЖАҚЫНДА",
    notify: "Хабарласам",
    lessonOf: (a, b) => `${a}-сабақ / ${b}`,
    next: "Келесі:",
    friends: "Сыныптастарың онлайн",
    friendsSub: "5 дос қазір сабақ оқып жатыр",
    leaderboard: "Көшбасшылар",
    mascotMsg: "Қайта оралғаныңа қуаныштымын! Бүгін көбейту кестесін бітірейік 🚀",
    level: "Деңгей",
  },
  ru: {
    welcome: "Привет, Али!",
    welcomeSub: "Что изучим сегодня?",
    eyebrowPick: "ВЫБЕРИ ПРЕДМЕТ",
    title: "Уроки для начальной школы",
    continueEyebrow: "Продолжить",
    continueTitle: "Сложение · до 100 · урок 1",
    continueSub: "Ты занимался этим вчера. Осталось 5 минут.",
    continueBtn: "Продолжить урок",
    quests: "Задания на сегодня",
    questsPill: "3 / 4",
    q1: "Закончи один урок",    q1m: "≈ 8 мин",
    q2: "Повтори 20 карточек",  q2m: "Казахский",
    q3: "Пройди викторину",     q3m: "Математика",
    q4: "Играй с друзьями",     q4m: "Новое!",
    eyebrowAll: "ВСЕ ПРЕДМЕТЫ",
    all: "Все предметы",
    seeAll: "Смотреть всё",
    soon: "СКОРО",
    notify: "Сообщить",
    lessonOf: (a, b) => `Урок ${a} из ${b}`,
    next: "Далее:",
    friends: "Одноклассники онлайн",
    friendsSub: "5 друзей сейчас учатся",
    leaderboard: "Рейтинг",
    mascotMsg: "С возвращением! Давай добьём таблицу умножения 🚀",
    level: "Уровень",
  },
  en: {
    welcome: "Hi, Ali!",
    welcomeSub: "What shall we learn today?",
    eyebrowPick: "PICK A SUBJECT",
    title: "Lessons for primary school",
    continueEyebrow: "Continue",
    continueTitle: "Addition · within 100 · Lesson 1",
    continueSub: "You spent time on this yesterday. Just 5 minutes left.",
    continueBtn: "Resume lesson",
    quests: "Today's quests",
    questsPill: "3 / 4",
    q1: "Finish a lesson",   q1m: "≈ 8 min",
    q2: "Review 20 cards",   q2m: "Kazakh",
    q3: "Take one quiz",     q3m: "Math",
    q4: "Play with friends", q4m: "New!",
    eyebrowAll: "ALL SUBJECTS",
    all: "All subjects",
    seeAll: "See all",
    soon: "SOON",
    notify: "Notify me",
    lessonOf: (a, b) => `Lesson ${a} of ${b}`,
    next: "Next:",
    friends: "Classmates online",
    friendsSub: "5 friends are learning right now",
    leaderboard: "Leaderboard",
    mascotMsg: "Welcome back! Let's finish times tables today 🚀",
    level: "Level",
  },
};

// ─── Subject catalogue ─────────────────────────────────────────────

const SUB_CONTENT = {
  kk: [
    { id:"math", name:"Математика", tag:"Қосу, алу, көбейту", color:"math", icon: IconMath, ready:true,
      lessonTitles:{ 1:"Қосу · 100 ішінде", 2:"Алу · 100 ішінде", 3:"Көбейту кестесі · 2-ге", 4:"Көбейту кестесі · 3-ке" } },
    { id:"kaz",  name:"Қазақ тілі", tag:"Әліпби, дыбыстар, сөздер", color:"kaz", icon: IconKaz, ready:true,
      lessonTitles:{ 4:"Жуан және жіңішке дыбыстар", 5:"Сөз құрастыру" } },
    { id:"world", name:"Дүниетану", tag:"Табиғат, жануарлар", color:"world", icon: IconWorld, ready:false, days:12 },
    { id:"eng",   name:"English",   tag:"Сөздер мен сөйлемдер", color:"eng", icon: IconEng, ready:false, days:24 },
  ],
  ru: [
    { id:"math", name:"Математика", tag:"Сложение, вычитание, умножение", color:"math", icon: IconMath, ready:true,
      lessonTitles:{ 1:"Сложение · до 100", 2:"Вычитание · до 100", 3:"Таблица умножения · на 2", 4:"Таблица умножения · на 3" } },
    { id:"kaz",  name:"Казахский",  tag:"Алфавит, звуки, слова", color:"kaz", icon: IconKaz, ready:true,
      lessonTitles:{ 4:"Твёрдые и мягкие звуки", 5:"Составь слово" } },
    { id:"world", name:"Познание мира", tag:"Природа, животные", color:"world", icon: IconWorld, ready:false, days:12 },
    { id:"eng",   name:"English",       tag:"Слова и предложения", color:"eng", icon: IconEng, ready:false, days:24 },
  ],
  en: [
    { id:"math", name:"Math",   tag:"Addition, subtraction, times tables", color:"math", icon: IconMath, ready:true,
      lessonTitles:{ 1:"Addition · within 100", 2:"Subtraction · within 100", 3:"Times tables · ×2", 4:"Times tables · ×3" } },
    { id:"kaz",  name:"Kazakh", tag:"Alphabet, sounds, words", color:"kaz", icon: IconKaz, ready:true,
      lessonTitles:{ 4:"Hard & soft vowels", 5:"Build a word" } },
    { id:"world", name:"World Studies", tag:"Nature, animals", color:"world", icon: IconWorld, ready:false, days:12 },
    { id:"eng",   name:"English",       tag:"Words and sentences", color:"eng", icon: IconEng, ready:false, days:24 },
  ],
};

const LESSON_FOR = (subjectId, lessonNum) => {
  if (subjectId === 'math') return `math-${lessonNum}`;
  if (subjectId === 'kaz')  return `kaz-${lessonNum}`;
  return null;
};

// ─── Progress ──────────────────────────────────────────────────────

const DEFAULT_PROGRESS = {
  math: { lesson: 1, stars: 0, of: 4 },
  kaz:  { lesson: 4, stars: 1, of: 18 },
  totalXP: 310,
  level: 7,
  streak: 12,
  lastPlayed: null,
  questsDone: [true, true, true, false],
};

const PROGRESS_KEY = 'mektep_progress_v1';

function loadProgress() {
  try {
    const raw = localStorage.getItem(PROGRESS_KEY);
    if (raw) return { ...DEFAULT_PROGRESS, ...JSON.parse(raw) };
  } catch (e) {}
  return DEFAULT_PROGRESS;
}

function saveProgress(p) {
  try { localStorage.setItem(PROGRESS_KEY, JSON.stringify(p)); } catch (e) {}
}

function subjectsFor(lang, progress) {
  const prog = progress || DEFAULT_PROGRESS;
  return SUB_CONTENT[lang].map(s => {
    const p = prog[s.id];
    if (!s.ready) return s;
    const lessonNum = p?.lesson ?? 1;
    const nextTitle = s.lessonTitles[lessonNum] || s.lessonTitles[Object.keys(s.lessonTitles)[0]];
    return { ...s, lesson: lessonNum, of: p.of, stars: p.stars, next: nextTitle, lessonId: LESSON_FOR(s.id, lessonNum) };
  });
}

// ─── Language chip ─────────────────────────────────────────────────

function LangChip({ lang, onChange }) {
  const [open, setOpen] = useState(false);
  const flags = { kk: '🇰🇿', ru: '🇷🇺', en: '🇬🇧' };
  const names = { kk: 'Қазақша', ru: 'Русский', en: 'English' };
  return (
    <div className="lang-chip" onClick={() => setOpen(!open)} style={{ cursor: 'pointer', position: 'relative' }}>
      <span className="lang-flag">{flags[lang]}</span>
      <span>{names[lang]}</span>
      <span className="caret">▼</span>
      {open && (
        <div style={{ position: 'absolute', top: 'calc(100% + 6px)', right: 0, background: '#fff',
          borderRadius: 14, padding: 6, boxShadow: '0 10px 30px rgba(0,0,0,.12)',
          border: '1px solid var(--line)', minWidth: 160, zIndex: 10 }}>
          {Object.keys(names).map(k => (
            <div key={k} onClick={(e) => { e.stopPropagation(); onChange(k); setOpen(false); }}
              style={{ padding: '10px 12px', borderRadius: 10, display: 'flex', alignItems: 'center',
                gap: 8, background: k === lang ? 'var(--card-soft)' : 'transparent', cursor: 'pointer' }}>
              <span className="lang-flag">{flags[k]}</span>
              <span>{names[k]}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Subject card ──────────────────────────────────────────────────

function SubjectCard({ s, t, onOpen }) {
  const Icon = s.icon;
  if (!s.ready) {
    return (
      <div className="subject locked">
        <div className="subj-top">
          <div className="subj-ic" style={{ background: `var(--${s.color}-bg)`, color: `var(--${s.color}-ic)`, opacity: .7 }}><Icon /></div>
          <div className="soon-pill">{t.soon}</div>
        </div>
        <div className="locked-cnt">
          <div className="subj-name">{s.name}</div>
          <div className="subj-tag">{s.tag}</div>
          <div className="countdown">
            <div className="seg"><b>{s.days}</b><span>day</span></div>
            <div className="seg"><b>04</b><span>hr</span></div>
            <div className="seg"><b>22</b><span>min</span></div>
          </div>
          <button className="notify-btn">🔔 {t.notify}</button>
        </div>
      </div>
    );
  }
  const pct = Math.round((s.lesson / s.of) * 100);
  return (
    <div className="subject" onClick={onOpen}>
      <div className="subj-top">
        <div className="subj-ic" style={{ background: `var(--${s.color}-bg)`, color: `var(--${s.color}-ic)` }}><Icon /></div>
        <div className="subj-stars">{[0,1,2].map(i => <Star key={i} on={i < s.stars} />)}</div>
      </div>
      <div>
        <div className="subj-name">{s.name}</div>
        <div className="subj-tag">{s.tag}</div>
      </div>
      <div>
        <div className="subj-prog">
          <div className="subj-prog-row"><span>{t.lessonOf(s.lesson, s.of)}</span><span>{pct}%</span></div>
          <div className="subj-bar"><div style={{ width: pct + '%', background: `var(--${s.color}-ic)` }}></div></div>
        </div>
        <div className="subj-cta">
          <div className="next"><span className="dot" style={{ background: `var(--${s.color}-ic)` }}></span>{t.next} {s.next}</div>
          <div className="subj-go">→</div>
        </div>
      </div>
    </div>
  );
}

// ─── Lesson modal ──────────────────────────────────────────────────

function LessonModal({ s, t, onClose }) {
  if (!s) return null;
  const Icon = s.icon;
  const lessons = Array.from({ length: s.of }, (_, i) => ({
    n: i + 1,
    name: `${s.name} · ${t.next.replace(":", "")} ${i + 1}`,
    min: 6 + (i % 4) * 2,
    done: i + 1 < s.lesson,
    cur:  i + 1 === s.lesson,
  })).slice(s.lesson - 2, s.lesson + 3);
  return (
    <div className="modal-back" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <div className="modal-ic" style={{ background: `var(--${s.color}-bg)`, color: `var(--${s.color}-ic)` }}><Icon /></div>
          <div>
            <h3>{s.name}</h3>
            <div className="modal-sub">{t.lessonOf(s.lesson, s.of)} · {Math.round((s.lesson / s.of) * 100)}% complete</div>
          </div>
        </div>
        <div className="modal-list">
          {lessons.map(l => (
            <div key={l.n} className={"les " + (l.done ? "done " : "") + (l.cur ? "cur" : "")}>
              <div className="les-n">{l.done ? "✓" : l.n}</div>
              <div className="les-name">{l.cur ? s.next : l.name}</div>
              <div className="les-min">{l.min} min</div>
            </div>
          ))}
        </div>
        <div className="modal-foot">
          <button className="btn ghost" onClick={onClose}>Close</button>
          <button className="btn prim">▶ {t.continueBtn}</button>
        </div>
      </div>
    </div>
  );
}

// ─── Home screen ───────────────────────────────────────────────────

function HomeView({ tweaks, setTweak, progress, setProgress, onStartLesson }) {
  const t = L[tweaks.language];
  const subs = subjectsFor(tweaks.language, progress);
  const [open, setOpen] = useState(null);
  const quests = progress.questsDone;
  const setQuests = (q) => setProgress(p => ({ ...p, questsDone: q }));
  const subject = (id) => subs.find(s => s.id === id);

  return (
    <div className="v2">
      {/* ── top bar ── */}
      <div className="topbar">
        <div className="brand">
          <div className="brand-mark"><Sparkle style={{ color: '#fff' }} /></div>
          <div>
            <div className="brand-sub">{t.welcomeSub}</div>
            <div className="brand-name">{t.welcome}</div>
          </div>
        </div>
        <div className="topbar-right">
          <div className="chip streak"><div className="flame">🔥</div>{progress.streak}</div>
          <div className="chip xp">
            <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.1, alignItems: 'flex-start' }}>
              <span style={{ fontSize: 10, letterSpacing: '.1em', color: 'var(--ink-mute)', fontWeight: 800 }}>{t.level} {progress.level}</span>
              <span style={{ fontSize: 13, color: 'var(--ink)' }}>{progress.totalXP} XP</span>
            </div>
            <div className="xp-ring" style={{ '--pct': progress.totalXP % 100 }}>
              <span>{progress.totalXP % 100}</span>
            </div>
          </div>
          <LangChip lang={tweaks.language} onChange={(v) => setTweak('language', v)} />
          <div className="avatar">А</div>
        </div>
      </div>

      {/* ── hero: continue + quests ── */}
      <div className="hero">
        <div className="continue" style={{ cursor: 'pointer' }} onClick={() => {
          const m = subs.find(s => s.id === 'math');
          if (m?.lessonId) onStartLesson(m.lessonId);
        }}>
          <div>
            <div className="continue-eyebrow">{t.continueEyebrow}</div>
            <h2>{subs.find(s => s.id === 'math')?.next || t.continueTitle}</h2>
            <div className="sub">{t.continueSub}</div>
          </div>
          <div className="continue-row">
            <div className="continue-meta">
              <div className="continue-bar">
                <div style={{ width: ((progress.math.lesson / progress.math.of) * 100) + '%' }}></div>
              </div>
              <div className="continue-bar-text">
                <span>{t.lessonOf(progress.math.lesson, progress.math.of)}</span>
                <span>+45 XP</span>
              </div>
            </div>
            <button className="play-btn"><div className="ic">▶</div>{t.continueBtn}</button>
          </div>
        </div>

        <div className="quests">
          <div className="quests-head">
            <h3>{t.quests}</h3>
            <div className="quests-pill">{t.questsPill}</div>
          </div>
          {[
            { title: t.q1, meta: t.q1m, xp: "+10" },
            { title: t.q2, meta: t.q2m, xp: "+15" },
            { title: t.q3, meta: t.q3m, xp: "+20" },
            { title: t.q4, meta: t.q4m, xp: "+25" },
          ].map((q, i) => (
            <div key={i} className={"quest " + (quests[i] ? "done" : "")} style={{ cursor: 'pointer' }}
              onClick={() => { const c = [...quests]; c[i] = !c[i]; setQuests(c); }}>
              <div className="quest-check">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <path d="M4 12l5 5L20 6" stroke="currentColor" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </div>
              <div className="quest-body">
                <div className="quest-title">{q.title}</div>
                <div className="quest-meta">{q.meta}</div>
              </div>
              <div className="quest-reward">{q.xp} XP</div>
            </div>
          ))}
        </div>
      </div>

      {/* ── subject grid ── */}
      <div className="section-head">
        <div>
          <div className="eyebrow" style={{ fontSize: 11, letterSpacing: '.2em', fontWeight: 800, color: 'var(--brand)', textTransform: 'uppercase', marginBottom: 6 }}>{t.eyebrowAll}</div>
          <h2>{t.all}</h2>
        </div>
        <button className="more">{t.seeAll} →</button>
      </div>

      <div className="grid">
        {subs.map(s => (
          <SubjectCard key={s.id} s={s} t={t}
            onOpen={() => s.ready && s.lessonId && onStartLesson(s.lessonId)} />
        ))}
      </div>

      {/* ── friends strip ── */}
      <div className="friends">
        <div className="friends-l">
          <div className="stack">
            {[{bg:'#FFB547',tx:'Б'},{bg:'#5764D8',tx:'А'},{bg:'#E14B73',tx:'Д'},{bg:'#2D9D5C',tx:'М'},{bg:'var(--ink)',tx:'+1'}]
              .map((a, i) => <div key={i} className="a" style={{ background: a.bg }}>{a.tx}</div>)}
          </div>
          <div>
            <div className="friends-title">{t.friends}</div>
            <div className="friends-sub">{t.friendsSub}</div>
          </div>
        </div>
        <div className="friends-r">
          <button className="leaderboard-btn">🏆 {t.leaderboard}</button>
        </div>
      </div>

      {tweaks.mascot && (
        <div className="mascot">
          <div className="mascot-bubble">{t.mascotMsg}</div>
          <div className="mascot-body">🦊</div>
        </div>
      )}

      {open && <LessonModal s={subject(open)} t={t} onClose={() => setOpen(null)} />}
    </div>
  );
}

// ─── Root App ──────────────────────────────────────────────────────

function App() {
  const [tweaks, setTweakRaw] = useState(TWEAK_DEFAULTS);
  const setTweak = (key, val) => setTweakRaw(t => ({ ...t, [key]: val }));

  useEffect(() => {
    document.body.classList.toggle('dense', tweaks.density === 'dense');
    document.body.classList.toggle('no-mascot', !tweaks.mascot);
  }, [tweaks.density, tweaks.mascot]);

  const [showCallouts, setShowCallouts] = useState(true);
  const [progress, setProgressRaw] = useState(loadProgress);
  const [activeLesson, setActiveLesson]  = useState(null);
  const [toast, setToast] = useState(null);

  const setProgress = (updater) => {
    setProgressRaw(prev => {
      const next = typeof updater === 'function' ? updater(prev) : updater;
      saveProgress(next);
      return next;
    });
  };

  const showToast = (msg) => {
    setToast(msg);
    setTimeout(() => setToast(null), 2800);
  };

  const handleLessonComplete = ({ lessonId, correct, total, stars, xp }) => {
    const [subjectId, lessonNumStr] = lessonId.split('-');
    const lessonNum = parseInt(lessonNumStr, 10);
    setProgress(prev => {
      const subj = prev[subjectId] || { lesson: lessonNum, stars: 0, of: 24 };
      const advancing = lessonNum >= subj.lesson;
      const newLesson = advancing ? Math.min(subj.of, lessonNum + 1) : subj.lesson;
      const newStars  = advancing ? Math.max(subj.stars, stars) : subj.stars;
      const newXP     = prev.totalXP + xp;
      const newQuests = [...prev.questsDone];
      newQuests[0] = true;
      if (subjectId === 'kaz'  && !newQuests[1]) newQuests[1] = true;
      if (subjectId === 'math' && !newQuests[2]) newQuests[2] = true;
      return {
        ...prev,
        [subjectId]: { ...subj, lesson: newLesson, stars: newStars },
        totalXP: newXP,
        level: Math.floor(newXP / 100) + 1,
        lastPlayed: new Date().toISOString(),
        questsDone: newQuests,
      };
    });
    showToast(`+${xp} XP`);
  };

  // ── Active lesson ──
  if (activeLesson) {
    return (
      <>
        <LessonRunner
          lessonId={activeLesson}
          lang={tweaks.language}
          onClose={() => setActiveLesson(null)}
          onComplete={handleLessonComplete}
        />
        {toast && <div className="toast"><div className="ic">⚡</div><span>{toast}</span></div>}
      </>
    );
  }

  // ── Home screen ──
  return (
    <>
      <HomeView
        tweaks={tweaks} setTweak={setTweak}
        progress={progress} setProgress={setProgress}
        onStartLesson={(id) => setActiveLesson(id)}
      />
      {toast && <div className="toast"><div className="ic">⚡</div><span>{toast}</span></div>}

      {showCallouts && (
        <div className="callouts">
          <span className="x" onClick={() => setShowCallouts(false)}>✕</span>
          <h4>Try it</h4>
          <ul>
            <li>Tap the green "Continue" card</li>
            <li>Or tap Математика / Қазақ тілі</li>
            <li>Solve 6–10 problems · earn ★ + XP</li>
            <li>Progress saves automatically</li>
          </ul>
        </div>
      )}

      {TweaksPanel && (
        <TweaksPanel title="Tweaks">
          <TweakSection title="Language">
            <TweakSelect label="UI language" value={tweaks.language} onChange={(v) => setTweak('language', v)}
              options={[{value:'kk',label:'Қазақша'},{value:'ru',label:'Русский'},{value:'en',label:'English'}]} />
          </TweakSection>
          <TweakSection title="Personality">
            <TweakToggle label="Mascot" value={tweaks.mascot} onChange={(v) => setTweak('mascot', v)} />
            <TweakRadio label="Density" value={tweaks.density} onChange={(v) => setTweak('density', v)}
              options={[{value:'comfortable',label:'Comfortable'},{value:'dense',label:'Dense'}]} />
            <TweakToggle label="Show callouts" value={showCallouts} onChange={(v) => setShowCallouts(v)} />
          </TweakSection>
          <TweakSection title="Progress">
            <TweakButton label="Reset progress" onClick={() => {
              localStorage.removeItem(PROGRESS_KEY);
              setProgressRaw(DEFAULT_PROGRESS);
              showToast('Progress reset');
            }} />
          </TweakSection>
        </TweaksPanel>
      )}
    </>
  );
}

// ─── Export for sequential loader ──────────────────────────────────
window.App = App;
