async function requestJson(path, options) {
  const response = await fetch(path, options)
  const responseText = await response.text()
  let data

  if (responseText) {
    try {
      data = JSON.parse(responseText)
    } catch {
      if (response.ok) {
        throw new Error('The server returned an invalid response.')
      }
    }
  }

  if (!response.ok) {
    const message =
      typeof data?.message === 'string' && data.message.trim()
        ? data.message
        : `The request failed (${response.status}).`
    throw new Error(message)
  }

  return data
}

function validateQuestions(data) {
  if (
    !Array.isArray(data) ||
    data.some(
      (question) =>
        !question ||
        (typeof question.id !== 'number' && typeof question.id !== 'string') ||
        !String(question.id).trim() ||
        typeof question.questionText !== 'string' ||
        !question.questionText.trim() ||
        !Array.isArray(question.options) ||
        question.options.length < 2 ||
        question.options.some((option) => typeof option !== 'string' || !option.trim()),
    )
  ) {
    throw new Error('The questions endpoint returned data in an unexpected format.')
  }

  return data
}

function validateResult(data) {
  if (
    !data ||
    !Number.isInteger(data.score) ||
    !Number.isInteger(data.totalQuestions) ||
    data.totalQuestions < 1 ||
    data.score < 0 ||
    data.score > data.totalQuestions ||
    !Number.isFinite(data.percentage) ||
    data.percentage < 0 ||
    data.percentage > 100 ||
    !Array.isArray(data.correctAnswers) ||
    data.correctAnswers.length !== data.totalQuestions ||
    data.correctAnswers.some(
      (answer) =>
        !answer ||
        (typeof answer.questionId !== 'number' && typeof answer.questionId !== 'string') ||
        typeof answer.questionText !== 'string' ||
        typeof answer.selectedAnswer !== 'string' ||
        typeof answer.correctAnswer !== 'string' ||
        typeof answer.correct !== 'boolean',
    )
  ) {
    throw new Error('The submission endpoint returned data in an unexpected format.')
  }

  return data
}

export async function fetchQuestions() {
  const data = await requestJson('/api/quiz/questions')
  return validateQuestions(data)
}

export async function submitAnswers(answers) {
  const data = await requestJson('/api/quiz/submit', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ answers }),
  })

  return validateResult(data)
}

function validateQuizSummaries(data) {
  if (
    !Array.isArray(data) ||
    data.some(
      (quiz) =>
        !quiz ||
        !Number.isInteger(quiz.id) ||
        typeof quiz.title !== 'string' ||
        typeof quiz.description !== 'string' ||
        !Number.isInteger(quiz.questionCount) ||
        typeof quiz.published !== 'boolean',
    )
  ) {
    throw new Error('The quizzes endpoint returned data in an unexpected format.')
  }
  return data
}

export async function fetchTeacherQuizzes() {
  return validateQuizSummaries(await requestJson('/api/quizzes'))
}

export async function fetchAvailableQuizzes() {
  return validateQuizSummaries(await requestJson('/api/quizzes/available'))
}

export async function createQuiz(quiz) {
  const data = await requestJson('/api/quizzes', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(quiz),
  })
  return validateQuizSummaries([data])[0]
}

export async function fetchPlatformQuiz(quizId) {
  const data = await requestJson(`/api/quizzes/${quizId}/questions`)
  if (
    !data ||
    !Number.isInteger(data.id) ||
    typeof data.title !== 'string' ||
    typeof data.description !== 'string' ||
    !Array.isArray(data.questions) ||
    data.questions.length === 0 ||
    data.questions.some(
      (question) =>
        !question ||
        !Number.isInteger(question.id) ||
        typeof question.questionText !== 'string' ||
        !Array.isArray(question.options) ||
        question.options.length !== 4 ||
        question.options.some((option) => typeof option !== 'string') ||
        Object.hasOwn(question, 'correctAnswer'),
    )
  ) {
    throw new Error('The quiz questions endpoint returned data in an unexpected format.')
  }
  return data
}

export async function submitPlatformQuiz(quizId, student, answers) {
  const data = await requestJson(`/api/quizzes/${quizId}/submit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      studentName: student.name,
      studentId: student.id,
      answers,
    }),
  })
  return validateResult(data)
}

export async function fetchQuizResults(quizId) {
  const data = await requestJson(`/api/quizzes/${quizId}/results`)
  if (
    !Array.isArray(data) ||
    data.some(
      (attempt) =>
        !attempt ||
        !Number.isInteger(attempt.attemptId) ||
        !Number.isInteger(attempt.quizId) ||
        typeof attempt.quizTitle !== 'string' ||
        typeof attempt.studentName !== 'string' ||
        typeof attempt.studentId !== 'string' ||
        !Number.isInteger(attempt.score) ||
        !Number.isInteger(attempt.totalQuestions) ||
        !Number.isFinite(attempt.percentage) ||
        typeof attempt.submittedAt !== 'string',
    )
  ) {
    throw new Error('The quiz results endpoint returned data in an unexpected format.')
  }
  return data
}
