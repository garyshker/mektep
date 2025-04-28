let num1, num2, operator, correctAnswer;
let currentQuestion = 0;
let correctAnswers = 0;
const totalQuestions = 10;
let mode = 'basic';
let answered = false;

function startGame(selectedMode) {
  mode = selectedMode;
  document.getElementById('back-button').style.display = 'block'; // Жабшы батырмасы
  currentQuestion = 0;
  correctAnswers = 0;
  
  document.getElementById('mode-select').style.display = 'none';
  document.getElementById('game-area').style.display = 'block';

  // Бұл жолды соңына қарай қой немесе тексер:
  if (document.getElementById('restart-button')) {
    document.getElementById('restart-button').style.display = 'none';
  }

  generateQuestion();
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
  document.getElementById('title-heading').style.display = 'block';
}

function generateQuestion() {
  answered = false;

  // Очистка текста и стиля результата перед новым вопросом
  document.getElementById('result').textContent = '';
  document.getElementById('result').style.color = '';

  // Очистка предыдущих стилей кнопок
  const oldButtons = document.querySelectorAll('.option-button');
  oldButtons.forEach(btn => {
    btn.classList.remove('correct', 'wrong', 'disabled');
  });
  const isAddition = Math.random() < 0.5;
  operator = isAddition ? '+' : '-';

  if (mode === 'basic') {
    // Режим 0 - 10: простые числа от 0 до 10
    num1 = Math.floor(Math.random() * 11);
    num2 = Math.floor(Math.random() * 11);
  } else if (mode === 'tens') {
    // Режим 10 - 100: выбор из десятков
    const tens = [10,20,30,40,50,60,70,80,90,100];
    num1 = tens[Math.floor(Math.random() * tens.length)];
    num2 = tens[Math.floor(Math.random() * tens.length)];
  } else if (mode === 'money') {
    // Режим "Теңге": сложение/вычитание монет (5, 10, 20, 50, 100, 200)
    const moneyOptions = [1, 2, 5, 10, 20, 50, 100, 200];
    num1 = moneyOptions[Math.floor(Math.random() * moneyOptions.length)];
    num2 = moneyOptions[Math.floor(Math.random() * moneyOptions.length)];
    // Mode "Compare" Yes/No
  } else if (mode === 'compare') {
    num1 = Math.floor(Math.random() * 100) + 1;
    num2 = Math.floor(Math.random() * 100) + 1;
    if (num1 === num2) {
      num2 = (num2 % 100) + 1;
    }

// Показываем вопрос: "X > Y ?"
document.getElementById('question-label').textContent = `Сұрақ ${currentQuestion + 1}:`;
document.getElementById('math-problem').textContent = `${num1} > ${num2} ?`;

correctAnswer = num1 > num2 ? 'Иә' : 'Жоқ';

const optionsDiv = document.getElementById('options');
optionsDiv.innerHTML = '';

const answerOptions = ['Иә', 'Жоқ'];
answerOptions.sort(() => Math.random() - 0.5); // Перемешаем

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

return; // Остановим дальше выполнение
  }
  else if (mode === 'word') {
    const names = ["Али", "Мадина", "Айша", "Дамир", "Зере", "Алмас", "Алан", "Мерей", "Сұңқар", "Қасым"];
    const objects = ["алма", "кітап", "доп", "қалам", "сәбіз", "банан", "доллар"];

    const name = names[Math.floor(Math.random() * names.length)];
    const object = objects[Math.floor(Math.random() * objects.length)];
    let start = Math.floor(Math.random() * 10) + 1;
    let change = Math.floor(Math.random() * 10) + 1;

    // Определяем правильное окончание для имени
    let nameWithEnding = getCorrectEnding(name);

    if (!isAddition && start < change) {
      [start, change] = [change, start];
    }

    correctAnswer = isAddition ? start + change : start - change;

    document.getElementById('question-label').textContent = `Сұрақ ${currentQuestion + 1}:`;
    let sentence = '';
    if (isAddition) {
      sentence = `${nameWithEnding} ${start} ${object} бар еді. Ол тағы ${change} ${object} алды. ${nameWithEnding} қанша ${object} болды?`;
    } else {
      sentence = `${nameWithEnding} ${start} ${object} бар еді. Ол ${change} ${object} досына берді. ${nameWithEnding} қанша ${object} қалды?`;
    }
    document.getElementById('math-problem').textContent = sentence;

    const optionsDiv = document.getElementById('options');
    optionsDiv.innerHTML = '';

    let answers = [correctAnswer];
    while (answers.length < 3) {
      let wrongAnswer = correctAnswer + Math.floor(Math.random() * 5) - 2;
      if (wrongAnswer >= 0 && !answers.includes(wrongAnswer)) {
        answers.push(wrongAnswer);
      }
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
  }

  if (!isAddition && num1 < num2) [num1, num2] = [num2, num1];
  // Добавим режим money без изменения других структур
  correctAnswer = operator === '+' ? num1 + num2 : num1 - num2;

  document.getElementById('question-label').textContent = `Сұрақ ${currentQuestion + 1}:`;
  if (mode === 'money') {
    const imagePath = 'img/'; // Папка, где находятся изображения монет
    document.getElementById('math-problem').innerHTML = `
      <img src="${imagePath}${num1}.webp" alt="${num1} теңге" class="coin-img">
      ${operator}
      <img src="${imagePath}${num2}.webp" alt="${num2} теңге" class="coin-img">
      = ?
    `;
  } else {
    // Удалена строка с textContent, так что здесь ничего не делаем
    document.getElementById('math-problem').textContent = `${num1} ${operator} ${num2} = ?`;
  }
  generateOptions();
}

function generateOptions() {
  const optionsDiv = document.getElementById('options');
  optionsDiv.innerHTML = '';
  let answers = [correctAnswer];
  while (answers.length < 3) {
    let wrongAnswer;
    // Генерация неправильных ответов для режима 10 - 100
    if (mode === 'tens') {
      const tensOptions = [10,20,30,40,50,60,70,80,90,100];
      wrongAnswer = tensOptions[Math.floor(Math.random() * tensOptions.length)];
    } else {
      wrongAnswer = correctAnswer + Math.floor(Math.random() * 11 - 5);
      // Генерация неправильных ответов для режима "Теңге" из фиксированного набора
      if (mode === 'money') {
        const sums = [];
        const coins = [1, 2, 5, 10, 20, 50, 100, 200];
        for (let i = 0; i < coins.length; i++) {
          for (let j = i; j < coins.length; j++) {
            let sum = coins[i] + coins[j];
            if (sum !== correctAnswer && !sums.includes(sum)) {
              sums.push(sum);
            }
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
  // Отображение вариантов ответа в виде кнопок
  for (let ans of answers) {
    let btn = document.createElement('button');
    btn.textContent = ans;
    btn.className = 'option-button';
    btn.onclick = () => {
      if (!answered) {
        answered = true;
        handleAnswer(ans, btn);
      }
    };
    optionsDiv.appendChild(btn);
  }
}

// Обработка ответа и отображение результата
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
  } else {
    result.innerHTML = `Талпын! <span style="font-size: 24px;">😕</span> Дұрыс жауап: ${correctAnswer}`;
    result.style.color = 'red';
    buttonEl.classList.add('wrong');
    delay = 2000;
  }

  currentQuestion++;
  if (currentQuestion < totalQuestions) {
    setTimeout(generateQuestion, delay);
  } else {
    setTimeout(() => {
      document.getElementById('question-label').textContent = '';
      document.getElementById('math-problem').innerHTML = `Сен ${totalQuestions} сұрақтың ${correctAnswers} дұрыс жауап бердің. Жарайсың! 🎉`;
      document.getElementById('options').innerHTML = '';
    
      // Финал кезінде нәтижені тазалау
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
// Функция для правильного склонения имени (Мерейде, Аланда, Қасымда и т.д.)
function getCorrectEnding(name) {
  const lastChar = name.slice(-1).toLowerCase();
  const lastTwoChars = name.slice(-2).toLowerCase();
  const vowels = ['а', 'ә', 'ө', 'і', 'ү', 'ұ', 'ы', 'о', 'и', 'у', 'э', 'я', 'ю'];

  if (lastTwoChars === 'ей') {
    return name + 'де';
  } else if (lastChar === 'е' || lastChar === 'н' || lastChar === 'р') {
    return name + 'де';
  } else if (vowels.includes(lastChar)) {
    return name + 'да';
  } else {
    return name + 'та';
  }
}