// Lesson Runner — interactive lesson player for Mektep
// Exports: LessonRunner, LESSONS

const { useState, useEffect, useRef, useMemo } = React;

// ────────────────────────────────────────────────────────────────────
// LESSON CONTENT
// ────────────────────────────────────────────────────────────────────

// Each lesson is { id, subjectId, title, titleByLang, intro?, questions:[...] }
// Question kinds:
//   mc       { kind:'mc', prompt, big?, options:[...], answer:idx, hint? }
//   type     { kind:'type', prompt, big?, answer:string|number, units? }
//   match    { kind:'match', prompt, left:[...], right:[...], pairs:[[L,R],...] }
//   tap      { kind:'tap', prompt, words:[...], correctIdxs:[...] }   // tap all matching items
//   word     { kind:'word', story, prompt, options:[...], answer:idx }

const LESSONS = {
  // ───── Math · Lesson 7 — Times tables ×7 ─────
  "math-7": {
    id: "math-7",
    subjectId: "math",
    icon: "math",
    titleByLang: {
      kk: "Көбейту кестесі · 7",
      ru: "Таблица умножения · 7",
      en: "Times Tables · 7",
    },
    introByLang: {
      kk: "Бүгін 7-ге көбейтуді үйренеміз. Дайынсың ба?",
      ru: "Сегодня учим умножение на 7. Готова?",
      en: "Today we learn the 7 times table. Ready?",
    },
    questions: [
      { kind:"mc", big:true, prompt:"7 × 2", options:["12","14","16","21"], answer:1 },
      { kind:"mc", big:true, prompt:"7 × 3", options:["18","20","21","24"], answer:2 },
      { kind:"type", prompt:"7 × 4 = ?", answer:28 },
      { kind:"mc", big:true, prompt:"7 × 5", options:["28","35","42","45"], answer:1 },
      { kind:"tap",
        promptByLang:{
          kk:"7-нің көбейтінділерін тап",
          ru:"Найди произведения числа 7",
          en:"Tap all multiples of 7"
        },
        words:["14","18","21","24","28","30","35","40"],
        correctIdxs:[0,2,4,6] },
      { kind:"type", prompt:"7 × 6 = ?", answer:42 },
      { kind:"mc", big:true, prompt:"7 × 7", options:["42","48","49","56"], answer:2 },
      { kind:"word",
        storyByLang:{
          kk:"Алмада 7 қалта бар. Әр қалтада 8 алма. Барлығы қанша алма?",
          ru:"У Алмы 7 пакетов. В каждом по 8 яблок. Сколько всего яблок?",
          en:"Alma has 7 bags. Each bag has 8 apples. How many apples in total?"
        },
        options:["49","54","56","64"], answer:2 },
      { kind:"type", prompt:"7 × 9 = ?", answer:63 },
      { kind:"mc", big:true, prompt:"7 × 10", options:["60","70","77","100"], answer:1 },
    ]
  },

  // ───── Math · Lesson 8 — Word problems ─────
  "math-8": {
    id: "math-8",
    subjectId: "math",
    icon: "math",
    titleByLang: {
      kk: "Мәтінді есептер · 8",
      ru: "Текстовые задачи · 8",
      en: "Word Problems · 8",
    },
    questions: [
      { kind:"word",
        storyByLang:{
          kk:"Дүкенде 24 теңге тұратын қалам бар. Айдана 3 қалам сатып алды. Қанша теңге төлейді?",
          ru:"В магазине ручка стоит 24 тенге. Айдана купила 3 ручки. Сколько тенге она заплатит?",
          en:"A pen costs 24 tenge. Aidana buys 3 pens. How much does she pay?"
        },
        options:["48 ₸","56 ₸","72 ₸","84 ₸"], answer:2 },
      { kind:"type", prompt:"45 + 28 = ?", answer:73 },
      { kind:"type", prompt:"100 - 37 = ?", answer:63 },
      { kind:"mc", big:true, prompt:"6 × 8", options:["42","46","48","54"], answer:2 },
      { kind:"word",
        storyByLang:{
          kk:"Сыныпта 18 қыз бен 14 ұл бар. Барлығы қанша оқушы?",
          ru:"В классе 18 девочек и 14 мальчиков. Сколько всего учеников?",
          en:"There are 18 girls and 14 boys in the class. How many pupils in total?"
        },
        options:["28","30","32","34"], answer:2 },
      { kind:"type", prompt:"81 ÷ 9 = ?", answer:9 },
    ]
  },

  // ───── Kazakh · Lesson 4 — Hard & soft vowels ─────
  "kaz-4": {
    id: "kaz-4",
    subjectId: "kaz",
    icon: "kaz",
    titleByLang: {
      kk: "Жуан және жіңішке дыбыстар",
      ru: "Твёрдые и мягкие гласные",
      en: "Hard & Soft Vowels",
    },
    introByLang: {
      kk: "Қазақ тілінде дауысты дыбыстар жуан немесе жіңішке болады.",
      ru: "В казахском гласные бывают твёрдыми или мягкими.",
      en: "In Kazakh, vowels are either hard or soft.",
    },
    questions: [
      { kind:"mc",
        promptByLang:{
          kk:"Жуан дауысты дыбыс қайсы?",
          ru:"Какой гласный твёрдый?",
          en:"Which vowel is hard?"
        },
        options:["ә","і","а","ү"], answer:2 },
      { kind:"mc",
        promptByLang:{
          kk:"Жіңішке дауысты дыбыс қайсы?",
          ru:"Какой гласный мягкий?",
          en:"Which vowel is soft?"
        },
        options:["а","о","ұ","ө"], answer:3 },
      { kind:"tap",
        promptByLang:{
          kk:"Барлық жуан дыбыстарды тап",
          ru:"Найди все твёрдые гласные",
          en:"Tap all hard vowels"
        },
        words:["а","ә","о","ө","ұ","ү","ы","і"],
        correctIdxs:[0,2,4,6] },
      { kind:"match",
        promptByLang:{
          kk:"Сөзді дұрыс топқа жатқыз",
          ru:"Соедини слово с группой",
          en:"Match each word to its group"
        },
        groupsByLang:{
          kk:["Жуан","Жіңішке"],
          ru:["Твёрдые","Мягкие"],
          en:["Hard","Soft"]
        },
        items:[
          { text:"бала", group:0 },
          { text:"күн",  group:1 },
          { text:"тау",  group:0 },
          { text:"көл",  group:1 },
          { text:"қол",  group:0 },
          { text:"бөрі", group:1 },
        ] },
      { kind:"mc",
        promptByLang:{
          kk:"Қай сөз жіңішке?",
          ru:"Какое слово мягкое?",
          en:"Which word is soft?"
        },
        options:["қалам","терезе","балапан","тау"], answer:1 },
      { kind:"mc",
        promptByLang:{
          kk:"Қай сөз жуан?",
          ru:"Какое слово твёрдое?",
          en:"Which word is hard?"
        },
        options:["сүт","піл","ат","іні"], answer:2 },
    ]
  },
};

// ────────────────────────────────────────────────────────────────────
// Localization
// ────────────────────────────────────────────────────────────────────
const RT = {
  kk: {
    check:"Тексеру", continue:"Жалғастыру", next:"Келесі",
    skip:"Өткізу",
    correct:"Дұрыс!", wrong:"Қателестің", tryAgain:"Қайталап көр",
    typeHere:"Жауабыңды жаз",
    lessonComplete:"Сабақ аяқталды!",
    perfect:"Тамаша!", great:"Жарайсың!", good:"Жақсы!",
    xpEarned:"XP алдың", accuracy:"Дәлдік", time:"Уақыт",
    finish:"Аяқтау",
    quit:"Шығу", quitConfirm:"Шынымен шыққың келе ме? Прогресс жоғалады.",
    yes:"Иә", no:"Жоқ",
    matchPrompt:"Жұптарды тауып ал",
    streakBonus:"+10 күн қатарынан",
  },
  ru: {
    check:"Проверить", continue:"Продолжить", next:"Дальше",
    skip:"Пропустить",
    correct:"Правильно!", wrong:"Неверно", tryAgain:"Попробуй ещё",
    typeHere:"Введи ответ",
    lessonComplete:"Урок пройден!",
    perfect:"Идеально!", great:"Отлично!", good:"Молодец!",
    xpEarned:"XP получено", accuracy:"Точность", time:"Время",
    finish:"Завершить",
    quit:"Выйти", quitConfirm:"Точно выйти? Прогресс пропадёт.",
    yes:"Да", no:"Нет",
    matchPrompt:"Найди пары",
    streakBonus:"+10 дней подряд",
  },
  en: {
    check:"Check", continue:"Continue", next:"Next",
    skip:"Skip",
    correct:"Correct!", wrong:"Not quite", tryAgain:"Try again",
    typeHere:"Type your answer",
    lessonComplete:"Lesson complete!",
    perfect:"Perfect!", great:"Great work!", good:"Nice!",
    xpEarned:"XP earned", accuracy:"Accuracy", time:"Time",
    finish:"Finish",
    quit:"Quit", quitConfirm:"Quit lesson? Progress will be lost.",
    yes:"Yes", no:"No",
    matchPrompt:"Find the pairs",
    streakBonus:"+10 day streak",
  }
};

const pickLang = (obj, lang) => obj?.[lang] ?? obj?.en ?? Object.values(obj || {})[0];

// ────────────────────────────────────────────────────────────────────
// Sub-components: each question kind renders + reports correctness
// ────────────────────────────────────────────────────────────────────

function QMC({ q, lang, locked, picked, onPick }) {
  const prompt = q.promptByLang ? pickLang(q.promptByLang, lang) : q.prompt;
  return (
    <div className="qbody">
      {q.big ? (
        <div className="big-prompt">{prompt}</div>
      ) : (
        <div className="text-prompt">{prompt}</div>
      )}
      <div className={"opts " + (q.big ? "grid-2" : "stack")}>
        {q.options.map((o, i) => {
          let cls = "opt";
          if (locked) {
            if (i === q.answer) cls += " right";
            else if (i === picked) cls += " wrong";
            else cls += " dim";
          } else if (i === picked) cls += " sel";
          return (
            <button key={i} className={cls} onClick={()=>!locked && onPick(i)}>
              <span className="opt-key">{String.fromCharCode(65+i)}</span>
              <span className="opt-text">{o}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function QType({ q, lang, locked, value, onChange, correct }) {
  const inputRef = useRef(null);
  useEffect(()=>{ if (!locked) setTimeout(()=>inputRef.current?.focus(), 80); }, [q, locked]);
  return (
    <div className="qbody">
      <div className="big-prompt">{q.prompt}</div>
      <div className="type-wrap">
        <input
          ref={inputRef}
          className={"type-input " + (locked ? (correct ? "right" : "wrong") : "")}
          placeholder={pickLang(RT, lang).typeHere}
          value={value ?? ""}
          disabled={locked}
          onChange={(e)=>onChange(e.target.value)}
          inputMode="numeric"
        />
        {q.units && <span className="units">{q.units}</span>}
      </div>
    </div>
  );
}

function QTap({ q, lang, locked, picked, onToggle }) {
  const prompt = q.promptByLang ? pickLang(q.promptByLang, lang) : q.prompt;
  return (
    <div className="qbody">
      <div className="text-prompt">{prompt}</div>
      <div className="tap-grid">
        {q.words.map((w,i)=>{
          const isCorrectAns = q.correctIdxs.includes(i);
          const isPicked = picked.includes(i);
          let cls = "tap-w";
          if (locked) {
            if (isCorrectAns && isPicked) cls += " right";
            else if (isCorrectAns && !isPicked) cls += " missed";
            else if (!isCorrectAns && isPicked) cls += " wrong";
            else cls += " dim";
          } else if (isPicked) cls += " sel";
          return (
            <button key={i} className={cls} onClick={()=>!locked && onToggle(i)}>{w}</button>
          );
        })}
      </div>
    </div>
  );
}

function QWord({ q, lang, locked, picked, onPick }) {
  const story = pickLang(q.storyByLang, lang);
  return (
    <div className="qbody">
      <div className="story">
        <div className="story-ic">📖</div>
        <div className="story-text">{story}</div>
      </div>
      <div className="opts grid-2">
        {q.options.map((o, i) => {
          let cls = "opt";
          if (locked) {
            if (i === q.answer) cls += " right";
            else if (i === picked) cls += " wrong";
            else cls += " dim";
          } else if (i === picked) cls += " sel";
          return (
            <button key={i} className={cls} onClick={()=>!locked && onPick(i)}>
              <span className="opt-key">{String.fromCharCode(65+i)}</span>
              <span className="opt-text">{o}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

// Match: tap a word, then tap its group — pairs lock in
function QMatch({ q, lang, locked, state, setState }) {
  const prompt = q.promptByLang ? pickLang(q.promptByLang, lang) : q.prompt;
  const groups = pickLang(q.groupsByLang, lang);
  // state: { pairs: { itemIdx: groupIdx }, pendingItem: number|null }
  const pairs = state.pairs || {};
  const pending = state.pendingItem;

  const pickItem = (i) => {
    if (locked) return;
    if (pairs[i] !== undefined) return; // already placed
    setState({ ...state, pendingItem: pending === i ? null : i });
  };
  const pickGroup = (g) => {
    if (locked) return;
    if (pending === null || pending === undefined) return;
    setState({ pairs: { ...pairs, [pending]: g }, pendingItem: null });
  };

  return (
    <div className="qbody">
      <div className="text-prompt">{prompt}</div>
      <div className="match-wrap">
        <div className="match-items">
          {q.items.map((it, i)=>{
            const placed = pairs[i];
            let cls = "match-item";
            if (placed !== undefined) {
              if (locked) {
                cls += placed === it.group ? " right" : " wrong";
              } else {
                cls += " placed";
              }
            } else if (pending === i) cls += " active";
            return (
              <button key={i} className={cls} onClick={()=>pickItem(i)}>
                <span>{it.text}</span>
                {placed !== undefined && <span className="badge">{groups[placed]}</span>}
              </button>
            );
          })}
        </div>
        <div className="match-groups">
          {groups.map((g, gi)=>(
            <button key={gi} className={"match-group " + (pending !== null && pending !== undefined ? "ready":"")} onClick={()=>pickGroup(gi)}>
              <span className="g-name">{g}</span>
              <span className="g-count">{Object.values(pairs).filter(v=>v===gi).length}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────
// LessonRunner — orchestrates the lesson
// ────────────────────────────────────────────────────────────────────

function LessonRunner({ lessonId, lang, onClose, onComplete }) {
  const lesson = LESSONS[lessonId];
  const rt = RT[lang] || RT.en;
  const [idx, setIdx] = useState(0);
  const [hearts, setHearts] = useState(3);
  const [answers, setAnswers] = useState({});      // per question
  const [locked, setLocked] = useState(false);     // after Check pressed
  const [matchState, setMatchState] = useState({});// per match question
  const [feedback, setFeedback] = useState(null);  // 'right'|'wrong'|null
  const [done, setDone] = useState(false);
  const [startedAt] = useState(Date.now());
  const [correctCount, setCorrectCount] = useState(0);
  const [showQuit, setShowQuit] = useState(false);

  const total = lesson.questions.length;
  const q = lesson.questions[idx];
  const progressPct = ((idx + (locked?1:0)) / total) * 100;

  const setAns = (v) => setAnswers(a => ({ ...a, [idx]: v }));
  const current = answers[idx];
  const currentMatch = matchState[idx] || { pairs:{}, pendingItem:null };
  const setCurrentMatch = (s) => setMatchState(m => ({ ...m, [idx]: s }));

  // Can the user submit?
  const canCheck = (() => {
    if (q.kind === 'mc' || q.kind === 'word') return current !== undefined && current !== null;
    if (q.kind === 'type') return current !== undefined && String(current).trim() !== "";
    if (q.kind === 'tap') return Array.isArray(current) && current.length > 0;
    if (q.kind === 'match') return Object.keys(currentMatch.pairs||{}).length === q.items.length;
    return false;
  })();

  // Evaluate
  const evaluate = () => {
    if (q.kind === 'mc' || q.kind === 'word') return current === q.answer;
    if (q.kind === 'type') return Number(current) === Number(q.answer);
    if (q.kind === 'tap') {
      const want = [...q.correctIdxs].sort();
      const have = [...current].sort();
      return want.length === have.length && want.every((v,i)=>v===have[i]);
    }
    if (q.kind === 'match') {
      return q.items.every((it, i) => currentMatch.pairs[i] === it.group);
    }
    return false;
  };

  const check = () => {
    const right = evaluate();
    setLocked(true);
    setFeedback(right ? 'right' : 'wrong');
    if (right) {
      setCorrectCount(c => c+1);
    } else {
      setHearts(h => Math.max(0, h-1));
    }
  };

  const advance = () => {
    if (idx + 1 >= total || hearts === 0) {
      setDone(true);
      const accuracy = Math.round(((correctCount + (feedback==='right'?0:0)) / total) * 100);
      // We use correctCount already updated above
      const finalCorrect = correctCount; // already counted
      const stars = finalCorrect >= total - 0 ? 3 : finalCorrect >= total - 2 ? 2 : 1;
      const xp = 15 + finalCorrect * 5 + (stars === 3 ? 15 : stars === 2 ? 5 : 0);
      onComplete?.({ lessonId, correct: finalCorrect, total, stars, xp, hearts });
    } else {
      setIdx(idx+1);
      setLocked(false);
      setFeedback(null);
    }
  };

  // Keyboard: Enter to check/advance
  useEffect(() => {
    const h = (e) => {
      if (e.key === 'Enter') {
        if (!locked && canCheck) check();
        else if (locked) advance();
      } else if (e.key === 'Escape') {
        setShowQuit(true);
      }
    };
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  });

  if (done) {
    const finalCorrect = correctCount;
    const accuracy = Math.round((finalCorrect / total) * 100);
    const stars = finalCorrect >= total ? 3 : finalCorrect >= total - 2 ? 2 : 1;
    const xp = 15 + finalCorrect * 5 + (stars === 3 ? 15 : stars === 2 ? 5 : 0);
    const elapsed = Math.round((Date.now() - startedAt)/1000);
    const banner = stars === 3 ? rt.perfect : stars === 2 ? rt.great : rt.good;
    return (
      <CompleteScreen
        lesson={lesson} lang={lang} rt={rt}
        stars={stars} xp={xp} accuracy={accuracy}
        elapsed={elapsed} correct={finalCorrect} total={total}
        banner={banner}
        onFinish={onClose}
      />
    );
  }

  const title = pickLang(lesson.titleByLang, lang);

  return (
    <div className="lesson-shell">
      {/* top bar */}
      <div className="lesson-top">
        <button className="lt-close" onClick={()=>setShowQuit(true)} aria-label="Quit">✕</button>
        <div className="lt-bar">
          <div className="lt-bar-fill" style={{width: progressPct + '%'}} />
        </div>
        <div className="lt-hearts">
          {[0,1,2].map(i => (
            <span key={i} className={"heart " + (i < hearts ? "on":"off")}>♥</span>
          ))}
        </div>
      </div>

      <div className="lesson-stage">
        <div className="lesson-title-row">
          <div className="lt-eyebrow">{title}</div>
          <div className="lt-counter">{idx+1} / {total}</div>
        </div>

        {q.kind === 'mc' && <QMC q={q} lang={lang} locked={locked} picked={current} onPick={setAns} />}
        {q.kind === 'word' && <QWord q={q} lang={lang} locked={locked} picked={current} onPick={setAns} />}
        {q.kind === 'type' && <QType q={q} lang={lang} locked={locked} value={current} onChange={setAns} correct={feedback==='right'} />}
        {q.kind === 'tap' && <QTap q={q} lang={lang} locked={locked}
          picked={current || []}
          onToggle={(i)=>{
            const arr = current ? [...current] : [];
            const at = arr.indexOf(i);
            if (at >= 0) arr.splice(at,1); else arr.push(i);
            setAns(arr);
          }} />}
        {q.kind === 'match' && <QMatch q={q} lang={lang} locked={locked}
          state={currentMatch} setState={setCurrentMatch} />}
      </div>

      {/* feedback strip */}
      <div className={"feedback " + (feedback || "")}>
        {feedback === 'right' && (
          <div className="fb-inner">
            <div className="fb-ic ok">✓</div>
            <div className="fb-msg">
              <div className="fb-title">{rt.correct}</div>
              <div className="fb-sub">+5 XP</div>
            </div>
            <button className="fb-btn" onClick={advance}>{rt.continue}</button>
          </div>
        )}
        {feedback === 'wrong' && (
          <div className="fb-inner">
            <div className="fb-ic no">✕</div>
            <div className="fb-msg">
              <div className="fb-title">{rt.wrong}</div>
              <div className="fb-sub">{rightAnswerText(q, lang)}</div>
            </div>
            <button className="fb-btn" onClick={advance}>{rt.continue}</button>
          </div>
        )}
        {!feedback && (
          <div className="fb-inner">
            <button className="fb-btn ghost" onClick={advance}>{rt.skip}</button>
            <button className={"fb-btn primary " + (canCheck ? "" : "disabled")} disabled={!canCheck} onClick={check}>
              {rt.check}
            </button>
          </div>
        )}
      </div>

      {showQuit && (
        <div className="qmodal-back" onClick={()=>setShowQuit(false)}>
          <div className="qmodal" onClick={(e)=>e.stopPropagation()}>
            <h3>{rt.quitConfirm}</h3>
            <div className="qmodal-foot">
              <button className="btn ghost" onClick={()=>setShowQuit(false)}>{rt.no}</button>
              <button className="btn prim" onClick={onClose}>{rt.yes}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function rightAnswerText(q, lang) {
  if (q.kind === 'mc' || q.kind === 'word') return "→ " + q.options[q.answer];
  if (q.kind === 'type') return "→ " + q.answer;
  if (q.kind === 'tap') return "→ " + q.correctIdxs.map(i=>q.words[i]).join(", ");
  if (q.kind === 'match') return "—";
  return "";
}

// ────────────────────────────────────────────────────────────────────
// CompleteScreen — celebration + reward summary
// ────────────────────────────────────────────────────────────────────

function CompleteScreen({ lesson, lang, rt, stars, xp, accuracy, elapsed, correct, total, banner, onFinish }) {
  // confetti
  useEffect(()=>{
    const root = document.querySelector('.confetti-host');
    if (!root) return;
    const colors = ['#FFB547','#0E8C6B','#E14B73','#5764D8','#2D9D5C','#E88912'];
    for (let i=0; i<60; i++) {
      const el = document.createElement('div');
      el.className = 'confetti';
      el.style.left = Math.random()*100 + '%';
      el.style.background = colors[i % colors.length];
      el.style.animationDelay = (Math.random()*0.6) + 's';
      el.style.transform = `rotate(${Math.random()*360}deg)`;
      root.appendChild(el);
    }
    return ()=>{ root.innerHTML = ''; };
  }, []);

  const m = Math.floor(elapsed/60), s = elapsed % 60;
  const timeStr = m > 0 ? `${m}:${String(s).padStart(2,'0')}` : `${s}s`;
  const title = pickLang(lesson.titleByLang, lang);

  return (
    <div className="lesson-shell complete">
      <div className="confetti-host" />
      <div className="complete-card">
        <div className="celebrate">
          <div className="celebrate-emoji">🎉</div>
        </div>
        <div className="comp-stars">
          {[0,1,2].map(i => <span key={i} className={"big-star " + (i<stars?"on":"off")}>★</span>)}
        </div>
        <div className="comp-banner">{banner}</div>
        <div className="comp-sub">{rt.lessonComplete} · {title}</div>

        <div className="comp-stats">
          <div className="cs">
            <div className="cs-v">+{xp}</div>
            <div className="cs-l">{rt.xpEarned}</div>
          </div>
          <div className="cs">
            <div className="cs-v">{accuracy}%</div>
            <div className="cs-l">{rt.accuracy}</div>
          </div>
          <div className="cs">
            <div className="cs-v">{timeStr}</div>
            <div className="cs-l">{rt.time}</div>
          </div>
        </div>

        <button className="btn prim big" onClick={onFinish}>{rt.finish} →</button>
      </div>
    </div>
  );
}

// Expose to other Babel scripts
Object.assign(window, { LessonRunner, LESSONS, RT_LESSON: RT, pickLang });
