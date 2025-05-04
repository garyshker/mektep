
const translations = {
  kz: {
    greeting_title: "Сәлем досым!",
    greeting_text: "Кел, менімен бірге Математикаға қызығушылық таныт!",
    mode_basic: "1...10 сандар",
    mode_tens: "Ондық сандар",
    mode_triple: "a+b+c сандар",
    mode_money: "Теңге санау",
    mode_compare: "Салыстыру",
    mode_word: "Сөздік есеп"
  },
  ru: {
    greeting_title: "Привет, друг!",
    greeting_text: "Давай вместе увлечёмся математикой!",
    mode_basic: "Числа от 1 до 10",
    mode_tens: "Десятки",
    mode_triple: "a+b+c числа",
    mode_money: "Подсчёт тенге",
    mode_compare: "Сравнение",
    mode_word: "Текстовые задачи"
  },
  en: {
    greeting_title: "Hello friend!",
    greeting_text: "Let's get excited about math together!",
    mode_basic: "Numbers 1...10",
    mode_tens: "Tens",
    mode_triple: "a+b+c numbers",
    mode_money: "Count the money",
    mode_compare: "Compare",
    mode_word: "Word problems"
  }
};

function setLanguage(lang) {
  localStorage.setItem('lang', lang);
  location.reload();
}
function applyTranslations() {
  const lang = localStorage.getItem('lang') || 'kz';
  const dict = translations[lang];
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    if (dict[key]) el.textContent = dict[key];
  });
}

let currentMode = null;
let currentQuestion = null;
let score = 0;
let questionCount = 0;
const maxQuestions = 10;

const modeDescriptions = {
  basic: "1...10 сандар",
  tens: "Ондық сандар",
  triple: "a+b+c сандар",
  money: "Теңге санау",
  compare: "Салыстыру",
  word: "Сөздік есеп"
};

function startGame(mode) {
  currentMode = mode;
  score = 0;
  questionCount = 0;
  document.getElementById('mode-select').style.display = 'none';
  document.getElementById('intro').style.display = 'none';
  document.getElementById('game-area').style.display = 'block';
  document.getElementById('restart-button').style.display = 'none';
  document.getElementById('back-button').style.display = 'inline-block';
  document.getElementById('result').textContent = '';
  loadNextQuestion();
}

function returnToMenu() {
  currentMode = null;
  currentQuestion = null;
  score = 0;
  questionCount = 0;
  document.getElementById('mode-select').style.display = 'grid';
  document.getElementById('intro').style.display = 'block';
  document.getElementById('game-area').style.display = 'none';
  document.getElementById('restart-button').style.display = 'none';
  document.getElementById('back-button').style.display = 'none';
  document.getElementById('result').textContent = '';
}

function restartGame() {
  score = 0;
  questionCount = 0;
  document.getElementById('restart-button').style.display = 'none';
  document.getElementById('result').textContent = '';
  loadNextQuestion();
}

function loadNextQuestion() {
  if (questionCount >= maxQuestions) {
    endGame();
    return;
  }
  questionCount++;
  const lang = localStorage.getItem('lang') || 'kz';
  const modeNames = {
    kz: {
      basic: "1...10 сандар",
      tens: "Ондық сандар",
      triple: "a+b+c сандар",
      money: "Теңге санау",
      compare: "Салыстыру",
      word: "Сөздік есеп"
    },
    ru: {
      basic: "Числа от 1 до 10",
      tens: "Десятки",
      triple: "a+b+c числа",
      money: "Подсчёт тенге",
      compare: "Сравнение",
      word: "Текстовые задачи"
    },
    en: {
      basic: "Numbers 1...10",
      tens: "Tens",
      triple: "a+b+c numbers",
      money: "Count the money",
      compare: "Compare",
      word: "Word problems"
    }
  };
  document.getElementById('question-label').textContent =
    `${modeNames[lang][currentMode]} - ${lang === 'ru' ? 'вопрос' : lang === 'en' ? 'question' : 'сұрақ'} ${questionCount} / ${maxQuestions}`;
  document.getElementById('result').textContent = '';
  currentQuestion = generateQuestion(currentMode);
  displayQuestion(currentQuestion);
}

function generateQuestion(mode) {
  switch (mode) {
    case 'basic':
      return generateBasicQuestion();
    case 'tens':
      return generateTensQuestion();
    case 'triple':
      return generateTripleQuestion();
    case 'money':
      return generateMoneyQuestion();
    case 'compare':
      return generateCompareQuestion();
    case 'word':
      return generateWordProblem();
    default:
      return null;
  }
}

function generateBasicQuestion() {
  const a = Math.floor(Math.random() * 10) + 1;
  const b = Math.floor(Math.random() * 10) + 1;
  const sum = a + b;
  const options = generateOptions(sum, 10);
  return {
    problem: `${a} + ${b} = ?`,
    answer: sum,
    options
  };
}

function generateTensQuestion() {
  const a = (Math.floor(Math.random() * 9) + 1) * 10;
  const b = (Math.floor(Math.random() * 9) + 1) * 10;
  const sum = a + b;
  const options = generateOptions(sum, 100, 10);
  return {
    problem: `${a} + ${b} = ?`,
    answer: sum,
    options
  };
}

function generateTripleQuestion() {
  const a = Math.floor(Math.random() * 10) + 1;
  const b = Math.floor(Math.random() * 10) + 1;
  const c = Math.floor(Math.random() * 10) + 1;
  const sum = a + b + c;
  const options = generateOptions(sum, 20);
  return {
    problem: `${a} + ${b} + ${c} = ?`,
    answer: sum,
    options
  };
}

function generateMoneyQuestion() {
  const coins = [1, 2, 5, 10, 20, 50, 100, 200, 500];
  const a = coins[Math.floor(Math.random() * coins.length)];
  const b = coins[Math.floor(Math.random() * coins.length)];
  const sum = a + b;
  const options = generateOptions(sum, 1000, 50);
  return {
    problem: `${a} теңге + ${b} теңге = ?`,
    answer: sum,
    options
  };
}

function generateCompareQuestion() {
  const a = Math.floor(Math.random() * 50) + 1;
  const b = Math.floor(Math.random() * 50) + 1;
  const expression = `${a} > ${b}`;
  const lang = localStorage.getItem('lang') || 'kz';
  const answer = a > b
    ? (lang === 'ru' ? 'Да' : lang === 'en' ? 'Yes' : 'Иә')
    : (lang === 'ru' ? 'Нет' : lang === 'en' ? 'No' : 'Жоқ');
  const options =
    lang === 'ru' ? ['Да', 'Нет'] :
    lang === 'en' ? ['Yes', 'No'] :
    ['Иә', 'Жоқ'];
  return {
    problem: expression,
    answer,
    options
  };
}

function generateWordProblem() {
  const lang = localStorage.getItem('lang') || 'kz';
  const a = Math.floor(Math.random() * 10) + 1;
  const b = Math.floor(Math.random() * 10) + 1;
  const sum = a + b;

  let problem;
  if (lang === 'ru') {
    problem = `Если у тебя есть ${a} яблок и друг дал тебе ещё ${b}, сколько у тебя будет?`;
  } else if (lang === 'en') {
    problem = `If you have ${a} apples and your friend gives you ${b} more, how many will you have?`;
  } else {
    problem = `Егер сенде ${a} алма бар болса, және досың саған тағы ${b} алма берсе, сенде неше алма болады?`;
  }

  const options = generateOptions(sum, 20);
  return {
    problem,
    answer: sum,
    options
  };
}

function generateOptions(correctAnswer, maxOption, step=1) {
  let options = new Set();
  options.add(correctAnswer);
  while (options.size < 4) {
    let option = correctAnswer + (Math.floor(Math.random() * 10) - 5) * step;
    if (option > 0 && option <= maxOption) {
      options.add(option);
    }
  }
  let optionsArray = Array.from(options);
  return shuffleArray(optionsArray);
}

function shuffleArray(array) {
  for (let i = array.length -1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i+1));
    [array[i], array[j]] = [array[j], array[i]];
  }
  return array;
}

function displayQuestion(question) {
  const problemDiv = document.getElementById('math-problem');
  const optionsDiv = document.getElementById('options');
  problemDiv.innerHTML = question.problem;
  optionsDiv.innerHTML = '';
  question.options.forEach(option => {
    const btn = document.createElement('button');
    btn.className = 'option-button';
    btn.textContent = option;
    btn.onclick = () => checkAnswer(option);
    optionsDiv.appendChild(btn);
  });
}

function checkAnswer(selected) {
  const lang = localStorage.getItem('lang') || 'kz';
  if (currentMode === 'compare') {
    if (selected === currentQuestion.answer) {
      score++;
      document.getElementById('result').textContent = lang === 'ru' ? 'Верно!' : lang === 'en' ? 'Correct!' : 'Дұрыс!';
    } else {
      document.getElementById('result').textContent = lang === 'ru'
        ? `Неверно! Правильный ответ: ${currentQuestion.answer}`
        : lang === 'en'
        ? `Wrong! Correct answer: ${currentQuestion.answer}`
        : `Қате! Дұрыс жауап: ${currentQuestion.answer}`;
    }
  } else {
    if (selected === currentQuestion.answer) {
      score++;
      document.getElementById('result').textContent = lang === 'ru' ? 'Верно!' : lang === 'en' ? 'Correct!' : 'Дұрыс!';
    } else {
      document.getElementById('result').textContent = lang === 'ru'
        ? `Неверно! Правильный ответ: ${currentQuestion.answer}`
        : lang === 'en'
        ? `Wrong! Correct answer: ${currentQuestion.answer}`
        : `Қате! Дұрыс жауап: ${currentQuestion.answer}`;
    }
  }
  disableOptions();
  if (questionCount < maxQuestions) {
    setTimeout(loadNextQuestion, 1500);
  } else {
    setTimeout(endGame, 1500);
  }
}

function disableOptions() {
  const buttons = document.querySelectorAll('#options button');
  buttons.forEach(btn => btn.disabled = true);
}

function endGame() {
  const lang = localStorage.getItem('lang') || 'kz';
  document.getElementById('question-label').textContent =
    lang === 'ru' ? 'Игра окончена!' : lang === 'en' ? 'Game over!' : 'Ойын аяқталды!';
  document.getElementById('math-problem').textContent =
    lang === 'ru'
    ? `Ваш счёт: ${score} / ${maxQuestions}`
    : lang === 'en'
    ? `Your score: ${score} / ${maxQuestions}`
    : `Сіздің ұпайыңыз: ${score} / ${maxQuestions}`;
  document.getElementById('options').innerHTML = '';
  document.getElementById('restart-button').style.display = 'inline-block';
  document.getElementById('back-button').style.display = 'inline-block';
  document.getElementById('result').textContent = '';
}

document.querySelectorAll('.flags img').forEach(flag => {
  flag.addEventListener('click', () => {
    const lang = flag.getAttribute('data-lang');
    setLanguage(lang);
  });
});
applyTranslations();