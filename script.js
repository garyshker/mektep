let num1, num2, num3, operator, correctAnswer;
let currentQuestion = 0;
let correctAnswers = 0;
const totalQuestions = 10;
let mode = 'basic';
let answered = false;

function startGame(selectedMode) {
  mode = selectedMode;
  document.getElementById('back-button').style.display = 'block';
  currentQuestion = 0;
  correctAnswers = 0;

  document.getElementById('mode-select').style.display = 'none';
  document.getElementById('game-area').style.display = 'block';
  document.getElementById('intro').style.display = 'none';

  if (document.getElementById('restart-button')) {
    document.getElementById('restart-button').style.display = 'none';
  }

  generateQuestion();
}

function clearResult() {
  const result = document.getElementById('result');
  result.innerHTML = '';
  result.style.color = '';
  result.style.display = 'none';
  result.classList.remove('show');
}

function restartGame() {
  currentQuestion = 0;
  correctAnswers = 0;
  document.getElementById('restart-button').style.display = 'none';
  document.getElementById('options').innerHTML = '';
  document.getElementById('result').innerHTML = '';
  generateQuestion();
}

function returnToMenu() {
  document.getElementById('mode-select').style.display = 'block';
  document.getElementById('back-button').style.display = 'none';
  document.getElementById('game-area').style.display = 'none';
  document.getElementById('intro').style.display = 'block';
  document.querySelector('.mascot').style.transform = 'none';
  document.getElementById('intro').style.display = 'grid';
document.getElementById('mode-select').style.display = 'grid';
}

function generateQuestion() {
  clearResult();
  answered = false;

  const oldButtons = document.querySelectorAll('.option-button');
  oldButtons.forEach(btn => {
    btn.classList.remove('correct', 'wrong', 'disabled');
  });

  const isAddition = Math.random() < 0.5;
  operator = isAddition ? '+' : '-';

  if (mode === 'basic') {
    num1 = Math.floor(Math.random() * 11);
    num2 = Math.floor(Math.random() * 11);
  } else if (mode === 'tens') {
    const tens = [10,20,30,40,50,60,70,80,90,100];
    num1 = tens[Math.floor(Math.random() * tens.length)];
    num2 = tens[Math.floor(Math.random() * tens.length)];
  } else if (mode === 'money') {
    const moneyOptions = [1, 2, 5, 10, 20, 50, 100, 200];
    num1 = moneyOptions[Math.floor(Math.random() * moneyOptions.length)];
    num2 = moneyOptions[Math.floor(Math.random() * moneyOptions.length)];
  } else if (mode === 'compare') {
    num1 = Math.floor(Math.random() * 100) + 1;
    num2 = Math.floor(Math.random() * 100) + 1;
    if (num1 === num2) num2 = (num2 % 100) + 1;

    document.getElementById('question-label').textContent = `Сұрақ ${currentQuestion + 1}:`;
    document.getElementById('math-problem').textContent = `${num1} > ${num2} ?`;

    correctAnswer = num1 > num2 ? 'Иә' : 'Жоқ';

    const optionsDiv = document.getElementById('options');
    optionsDiv.innerHTML = '';
    const answerOptions = ['Иә', 'Жоқ'];
    answerOptions.sort(() => Math.random() - 0.5);
    answerOptions.forEach(ans => {
      const btn = document.createElement('button');
      btn.textContent = ans;
      btn.className = 'option-button';
      btn.onclick = () => {
        if (!answered) {
          answered = true;
          handleAnswer(ans, btn);
        }
      };
      optionsDiv.appendChild(btn);
    });
    return;
  } else if (mode === 'word') {
    const names = ["Али", "Мадина", "Айша", "Дамир", "Зере", "Алмас", "Алан", "Мерей", "Сұңқар", "Қасым", "Жетпісбай", "Қанат", "Жансая"];
    const objects = ["алма", "кітап", "доп", "қалам", "сәбіз", "банан", "доллар", "қызанақ", "қасық", "сағыз", "құлпынай", "түйме"];
    const name = names[Math.floor(Math.random() * names.length)];
    const object = objects[Math.floor(Math.random() * objects.length)];
    let start = Math.floor(Math.random() * 10) + 1;
    let change = Math.floor(Math.random() * 10) + 1;
    let nameWithEnding = getCorrectEnding(name);
    if (!isAddition && start < change) [start, change] = [change, start];

    correctAnswer = isAddition ? start + change : start - change;

    document.getElementById('question-label').textContent = `Сұрақ ${currentQuestion + 1}:`;
    let sentence = isAddition
      ? `${nameWithEnding} ${start} ${object} бар еді. Ол тағы ${change} ${object} сатып алды. ${nameWithEnding} қанша ${object} болды?`
      : `${nameWithEnding} ${start} ${object} бар еді. Ол ${change} ${object} досына берді. ${nameWithEnding} қанша ${object} қалды?`;

    document.getElementById('math-problem').textContent = sentence;
    const optionsDiv = document.getElementById('options');
    optionsDiv.innerHTML = '';
    let answers = [correctAnswer];
    while (answers.length < 3) {
      let wrongAnswer = correctAnswer + Math.floor(Math.random() * 5) - 2;
      if (wrongAnswer >= 0 && !answers.includes(wrongAnswer)) answers.push(wrongAnswer);
    }
    answers.sort(() => Math.random() - 0.5);
    answers.forEach(ans => {
      const btn = document.createElement('button');
      btn.className = 'option-button';
      btn.textContent = ans;
      btn.onclick = () => {
        if (!answered) {
          answered = true;
          handleAnswer(ans, btn);
        }
      };
      optionsDiv.appendChild(btn);
    });
    return;
  } else if (mode === 'triple') {
    num1 = Math.floor(Math.random() * 11);
    num2 = Math.floor(Math.random() * 11);
    num3 = Math.floor(Math.random() * 11);
    correctAnswer = num1 + num2 + num3;
    document.getElementById('question-label').textContent = `Сұрақ ${currentQuestion + 1}:`;
    document.getElementById('math-problem').textContent = `${num1} + ${num2} + ${num3} = ?`;
    generateOptions();
    return;
  }

  if (!isAddition && num1 < num2) [num1, num2] = [num2, num1];
  correctAnswer = operator === '+' ? num1 + num2 : num1 - num2;
  document.getElementById('question-label').textContent = `Сұрақ ${currentQuestion + 1}:`;

  if (mode === 'money') {
    const imagePath = 'img/';
    document.getElementById('math-problem').innerHTML = `
      <img src="${imagePath}${num1}.webp" alt="${num1} теңге" class="coin-img">
      ${operator}
      <img src="${imagePath}${num2}.webp" alt="${num2} теңге" class="coin-img">
      = ?
    `;
  } else {
    document.getElementById('math-problem').textContent = `${num1} ${operator} ${num2} = ?`;
  }

  generateOptions();
}

function generateOptions() {
  // Сброс классов для старых кнопок, чтобы предыдущие стили не сохранялись
  const oldButtons = document.querySelectorAll('.option-button');
  oldButtons.forEach(btn => {
    btn.classList.remove('correct', 'wrong', 'disabled');
  });
  const optionsDiv = document.getElementById('options');
  optionsDiv.innerHTML = '';
  let answers = [correctAnswer];
  while (answers.length < 3) {
    let wrongAnswer;
    if (mode === 'tens') {
      const tensOptions = [10,20,30,40,50,60,70,80,90,100];
      wrongAnswer = tensOptions[Math.floor(Math.random() * tensOptions.length)];
    } else {
      wrongAnswer = correctAnswer + Math.floor(Math.random() * 11 - 5);
      if (mode === 'money') {
        const sums = [];
        const coins = [1, 2, 5, 10, 20, 50, 100, 200];
        for (let i = 0; i < coins.length; i++) {
          for (let j = i; j < coins.length; j++) {
            let sum = coins[i] + coins[j];
            if (sum !== correctAnswer && !sums.includes(sum)) sums.push(sum);
          }
        }
        wrongAnswer = sums[Math.floor(Math.random() * sums.length)];
      }
    }
    if (!answers.includes(wrongAnswer) && wrongAnswer >= 0) {
      answers.push(wrongAnswer);
    }
  }
  answers.sort(() => Math.random() - 0.5);
  answers.forEach((ans, index) => {
    const btn = document.createElement('button');
    btn.textContent = ans;
    btn.className = 'option-button';
    btn.style.animationDelay = `${index * 0.1}s`;
    btn.onclick = () => {
      if (!answered) {
        answered = true;
        handleAnswer(ans, btn);
      }
    };
    optionsDiv.appendChild(btn);
  });
}

function handleAnswer(selected, buttonEl) {
  const result = document.getElementById('result');
  const buttons = document.querySelectorAll('.option-button');
  const isCorrect = (mode === 'compare') ? (selected === correctAnswer) : (selected === correctAnswer);
  let delay = 1200;

  buttons.forEach(btn => {
    btn.classList.add('disabled');
    if (parseInt(btn.textContent) === correctAnswer) {
      btn.classList.add('correct');
    }
  });

  if (selected === correctAnswer) {
    correctAnswers++;
    result.innerHTML = 'Тамаша! <span style="font-size: 24px;">😊</span>';
    result.style.color = 'green';
    result.style.display = 'block';
    result.classList.add('show');
  } else {
    result.innerHTML = `Талпын! <span style="font-size: 24px;">😕</span> Дұрыс жауап: ${correctAnswer}`;
    result.style.color = 'red';
    result.style.display = 'block';
    result.classList.add('show');
    buttonEl.classList.add('wrong');
    delay = 2000;
  }

  currentQuestion++;
  if (currentQuestion < totalQuestions) {
    setTimeout(() => {
      clearResult();
      generateQuestion();
    }, delay);
  } else {
    setTimeout(() => {
      document.getElementById('question-label').textContent = '';
      document.getElementById('math-problem').innerHTML = `Сен ${totalQuestions} сұрақтың ${correctAnswers} дұрыс жауап бердің. Жарайсың! 🎉`;
      document.getElementById('options').innerHTML = '';
      const resultEl = document.getElementById('result');
      if (resultEl) {
        resultEl.textContent = '';
        resultEl.style.color = '';
      }
      if (document.getElementById('restart-button')) {
        document.getElementById('restart-button').style.display = 'inline-block';
      }
    }, delay + 300);
  }
}

function getCorrectEnding(name) {
  const lastChar = name.slice(-1).toLowerCase();
  const lastTwoChars = name.slice(-2).toLowerCase();
  const vowels = ['а', 'ә', 'ө', 'і', 'ү', 'ұ', 'ы', 'о', 'и', 'у', 'э', 'я', 'ю'];
  if (lastTwoChars === 'ей' || lastTwoChars === 'ре' || lastTwoChars === 'ли') {
    return name + 'де';
  } else if (['ым', 'ар', 'ша', 'на', 'ая', 'ай'].includes(lastTwoChars)) {
    return name + 'да';
  } else if (['е', 'н', 'р', 'и'].includes(lastChar)) {
    return name + 'да';
  } else if (vowels.includes(lastChar)) {
    return name + 'де';
  } else {
    return name + 'та';
  }
}