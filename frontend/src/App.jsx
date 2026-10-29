import { useState } from 'react'
import {
  createQuiz,
  fetchAvailableQuizzes,
  fetchPlatformQuiz,
  fetchQuizResults,
  fetchTeacherQuizzes,
  submitPlatformQuiz,
} from './api.js'
import './App.css'

const blankQuestion = () => ({
  questionText: '',
  options: ['', '', '', ''],
  correctOption: '',
})

function formatDate(value) {
  if (!value) return 'Not available'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

function App() {
  const [view, setView] = useState('home')
  const [teacherTab, setTeacherTab] = useState('quizzes')
  const [teacherQuizzes, setTeacherQuizzes] = useState([])
  const [availableQuizzes, setAvailableQuizzes] = useState([])
  const [selectedQuiz, setSelectedQuiz] = useState(null)
  const [selectedQuizId, setSelectedQuizId] = useState('')
  const [attempts, setAttempts] = useState([])
  const [student, setStudent] = useState({ name: '', id: '' })
  const [studentForm, setStudentForm] = useState({ name: '', id: '' })
  const [answers, setAnswers] = useState({})
  const [result, setResult] = useState(null)
  const [quizForm, setQuizForm] = useState({
    title: '',
    description: '',
    questions: [blankQuestion()],
  })
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [validationMessage, setValidationMessage] = useState('')

  function goHome() {
    setView('home')
    setError('')
    setValidationMessage('')
  }

  async function openTeacherDashboard() {
    setView('teacher')
    setTeacherTab('quizzes')
    setLoading(true)
    setError('')
    try {
      setTeacherQuizzes(await fetchTeacherQuizzes())
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Could not load quizzes.')
    } finally {
      setLoading(false)
    }
  }

  async function retryTeacherQuizzes() {
    setTeacherTab('quizzes')
    setLoading(true)
    setError('')
    try {
      setTeacherQuizzes(await fetchTeacherQuizzes())
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Could not load quizzes.')
    } finally {
      setLoading(false)
    }
  }

  async function openStudentDashboard() {
    setView('student')
    setLoading(true)
    setError('')
    try {
      setAvailableQuizzes(await fetchAvailableQuizzes())
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Could not load published quizzes.')
    } finally {
      setLoading(false)
    }
  }

  async function handleStudentLogin(event) {
    event.preventDefault()
    const name = studentForm.name.trim()
    const id = studentForm.id.trim()
    if (!name || !id) {
      setValidationMessage('Enter both your name and student ID to continue.')
      return
    }
    setStudent({ name, id })
    setValidationMessage('')
    await openStudentDashboard({ name, id })
  }

  async function openQuiz(quizId) {
    setView('taking')
    setSelectedQuiz(null)
    setSelectedQuizId(String(quizId))
    setAnswers({})
    setResult(null)
    setValidationMessage('')
    setLoading(true)
    setError('')
    try {
      setSelectedQuiz(await fetchPlatformQuiz(quizId))
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Could not load this quiz.')
    } finally {
      setLoading(false)
    }
  }

  async function handleQuizSubmit(event) {
    event.preventDefault()
    if (!selectedQuiz || Object.keys(answers).length !== selectedQuiz.questions.length) {
      setValidationMessage(`Please answer all ${selectedQuiz?.questions.length ?? ''} questions before submitting.`)
      return
    }
    setValidationMessage('')
    setLoading(true)
    setError('')
    try {
      const submittedAnswers = selectedQuiz.questions.map((question) => ({
        questionId: question.id,
        selectedAnswer: answers[question.id],
      }))
      setResult(await submitPlatformQuiz(selectedQuiz.id, student, submittedAnswers))
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Could not submit your quiz.')
    } finally {
      setLoading(false)
    }
  }

  async function handleCreateQuiz(event) {
    event.preventDefault()
    setError('')
    const questions = quizForm.questions.map((question) => ({
      questionText: question.questionText.trim(),
      options: question.options.map((option) => option.trim()),
      correctAnswer: question.options[Number(question.correctOption)]?.trim() ?? '',
    }))
    if (
      questions.some(
        (question) =>
          !question.questionText ||
          question.options.some((option) => !option) ||
          !question.correctAnswer ||
          new Set(question.options).size !== 4,
      )
    ) {
      setValidationMessage('Complete every question, use four different options, and choose its correct answer.')
      return
    }
    setValidationMessage('')
    setSaving(true)
    try {
      const createdQuiz = await createQuiz({
        title: quizForm.title.trim(),
        description: quizForm.description.trim(),
        questions,
      })
      setTeacherQuizzes((current) => [createdQuiz, ...current])
      setTeacherTab('quizzes')
      setView('teacher')
      setQuizForm({ title: '', description: '', questions: [blankQuestion()] })
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Could not publish this quiz.')
    } finally {
      setSaving(false)
    }
  }

  async function loadResults(quizId) {
    setSelectedQuizId(String(quizId))
    setTeacherTab('results')
    setView('teacher')
    setAttempts([])
    setLoading(true)
    setError('')
    try {
      setAttempts(await fetchQuizResults(quizId))
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Could not load quiz results.')
    } finally {
      setLoading(false)
    }
  }

  async function retryResults() {
    if (selectedQuizId) await loadResults(selectedQuizId)
  }

  const questionCount = selectedQuiz?.questions.length ?? 0
  const answeredCount = Object.keys(answers).length
  const progress = questionCount ? (answeredCount / questionCount) * 100 : 0

  return (
    <div className="app-shell">
      <header className="topbar">
        <button className="brand brand-button" type="button" onClick={goHome} aria-label="QuizBox home">
          <span className="brand-mark" aria-hidden="true">QB</span>
          <span>QuizBox</span>
        </button>
        <div className="topbar-actions">
          {view !== 'home' && (
            <button className="topbar-link" type="button" onClick={goHome}>
              ← Home
            </button>
          )}
          {view === 'teacher' && <span className="topbar-note">Teacher demo</span>}
          {['student', 'taking'].includes(view) && student.name && (
            <span className="topbar-note">Student · {student.name}</span>
          )}
        </div>
      </header>

      <main className="page-content platform-content">
        {view === 'home' && (
          <>
            <section className="intro" aria-labelledby="page-title">
              <p className="eyebrow"><span className="eyebrow-line" />A PLACE TO ASK, ANSWER & LEARN</p>
              <h1 id="page-title">Welcome to QuizBox.</h1>
              <p className="intro-copy">Choose your space and make learning a little more interactive.</p>
            </section>
            <section className="role-grid" aria-label="Choose your role">
              <article className="role-card teacher-role">
                <span className="role-icon" aria-hidden="true">T</span>
                <p className="card-kicker">FOR EDUCATORS</p>
                <h2>Teacher Login</h2>
                <p>Create and publish quizzes, then see how your students did.</p>
                <p className="demo-notice">Demo access only — this is not secure authentication.</p>
                <button className="button button-primary role-button" type="button" onClick={openTeacherDashboard}>
                  Continue as demo teacher <span aria-hidden="true">→</span>
                </button>
              </article>
              <article className="role-card student-role">
                <span className="role-icon" aria-hidden="true">S</span>
                <p className="card-kicker">FOR STUDENTS</p>
                <h2>Student Login</h2>
                <p>Enter your college details to find a quiz and get started.</p>
                <form className="student-login-form" onSubmit={handleStudentLogin}>
                  <label>
                    Your name
                    <input
                      autoComplete="name"
                      maxLength={120}
                      required
                      value={studentForm.name}
                      onChange={(event) => setStudentForm({ ...studentForm, name: event.target.value })}
                      placeholder="e.g. Alex Student"
                    />
                  </label>
                  <label>
                    Student ID
                    <input
                      autoComplete="off"
                      maxLength={80}
                      required
                      value={studentForm.id}
                      onChange={(event) => setStudentForm({ ...studentForm, id: event.target.value })}
                      placeholder="e.g. S12345"
                    />
                  </label>
                  {validationMessage && <p className="inline-error" role="alert">{validationMessage}</p>}
                  <button className="button button-primary role-button" type="submit">
                    Continue as student <span aria-hidden="true">→</span>
                  </button>
                </form>
                <p className="demo-notice">Demo identity only — your name and ID are not verified.</p>
              </article>
            </section>
            <p className="demo-banner">
              <strong>College demo:</strong> Role selection and student ID entry identify a demo session only;
              they do not provide real authentication or account security.
            </p>
          </>
        )}

        {view === 'teacher' && (
          <>
            <section className="dashboard-heading">
              <div>
                <p className="eyebrow"><span className="eyebrow-line" />TEACHER DASHBOARD</p>
                <h1>Your quiz studio.</h1>
                <p>Build a quiz, publish it for students, and check the results.</p>
              </div>
              <span className="demo-pill">DEMO MODE</span>
            </section>
            <nav className="dashboard-tabs" aria-label="Teacher dashboard">
              <button
                className={teacherTab === 'quizzes' ? 'tab-active' : ''}
                type="button"
                onClick={retryTeacherQuizzes}
              >
                Your quizzes
              </button>
              <button
                className={teacherTab === 'create' ? 'tab-active' : ''}
                type="button"
                onClick={() => {
                  setTeacherTab('create')
                  setView('create')
                  setError('')
                }}
              >
                + Create quiz
              </button>
              {teacherTab === 'results' && <span className="tab-current">Quiz results</span>}
            </nav>
            {error && <ErrorNotice message={error} onRetry={teacherTab === 'results' ? retryResults : retryTeacherQuizzes} />}
            {loading ? (
              <LoadingCard label="Loading your teacher dashboard…" />
            ) : teacherTab === 'results' ? (
              <section className="panel results-panel">
                <div className="panel-heading">
                  <div>
                    <p className="card-kicker">STUDENT ATTEMPTS</p>
                    <h2>Quiz results</h2>
                  </div>
                  <label className="quiz-picker">
                    Select quiz
                    <select
                      value={selectedQuizId}
                      onChange={(event) => setSelectedQuizId(event.target.value)}
                    >
                      {teacherQuizzes.map((quiz) => (
                        <option key={quiz.id} value={quiz.id}>{quiz.title}</option>
                      ))}
                    </select>
                  </label>
                  <button
                    className="button button-secondary"
                    type="button"
                    disabled={!selectedQuizId}
                    onClick={() => selectedQuizId && loadResults(selectedQuizId)}
                  >
                    Refresh results
                  </button>
                </div>
                {attempts.length === 0 ? (
                  <EmptyState
                    title="No attempts yet"
                    text="Student results will appear here after someone submits this quiz."
                  />
                ) : (
                  <div className="table-scroll">
                    <table className="results-table">
                      <thead>
                        <tr>
                          <th>Student</th>
                          <th>Student ID</th>
                          <th>Quiz</th>
                          <th>Score</th>
                          <th>Percent</th>
                          <th>Submitted</th>
                        </tr>
                      </thead>
                      <tbody>
                        {attempts.map((attempt) => (
                          <tr key={attempt.attemptId}>
                            <td>{attempt.studentName}</td>
                            <td>{attempt.studentId}</td>
                            <td>{attempt.quizTitle}</td>
                            <td>{attempt.score} / {attempt.totalQuestions}</td>
                            <td><strong>{attempt.percentage}%</strong></td>
                            <td>{formatDate(attempt.submittedAt)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            ) : teacherQuizzes.length === 0 ? (
              <EmptyState
                title="Your quiz list is empty"
                text="Create your first quiz and it will be published for students."
                action={<button className="button button-primary" type="button" onClick={() => setView('create')}>Create a quiz</button>}
              />
            ) : (
              <section className="quiz-card-grid">
                {teacherQuizzes.map((quiz) => (
                  <article className="platform-quiz-card" key={quiz.id}>
                    <div className="quiz-card-top">
                      <span className="published-pill">PUBLISHED</span>
                      <span className="question-count">{quiz.questionCount} questions</span>
                    </div>
                    <h2>{quiz.title}</h2>
                    <p>{quiz.description}</p>
                    <div className="quiz-card-actions">
                      <button className="button button-secondary" type="button" onClick={() => loadResults(quiz.id)}>
                        View results
                      </button>
                      <span className="created-date">{formatDate(quiz.createdAt)}</span>
                    </div>
                  </article>
                ))}
              </section>
            )}
          </>
        )}

        {view === 'create' && (
          <>
            <section className="dashboard-heading">
              <div>
                <p className="eyebrow"><span className="eyebrow-line" />TEACHER DASHBOARD</p>
                <h1>Make a new quiz.</h1>
                <p>Your quiz is published as soon as it is saved.</p>
              </div>
              <span className="demo-pill">DEMO MODE</span>
            </section>
            <nav className="dashboard-tabs" aria-label="Teacher dashboard">
              <button type="button" onClick={openTeacherDashboard}>Your quizzes</button>
              <button className="tab-active" type="button">+ Create quiz</button>
            </nav>
            {error && <ErrorNotice message={error} />}
            <form className="panel quiz-builder" onSubmit={handleCreateQuiz}>
              <div className="builder-section">
                <p className="card-kicker">QUIZ DETAILS</p>
                <label className="form-label">
                  Quiz title
                  <input
                    required
                    maxLength={160}
                    value={quizForm.title}
                    onChange={(event) => setQuizForm({ ...quizForm, title: event.target.value })}
                    placeholder="e.g. Introduction to Biology"
                  />
                </label>
                <label className="form-label">
                  Description
                  <textarea
                    required
                    maxLength={2000}
                    rows={3}
                    value={quizForm.description}
                    onChange={(event) => setQuizForm({ ...quizForm, description: event.target.value })}
                    placeholder="What should students know about this quiz?"
                  />
                </label>
              </div>
              <div className="builder-section questions-builder">
                <div className="panel-heading">
                  <div>
                    <p className="card-kicker">QUESTION SETUP</p>
                    <h2>Your questions</h2>
                  </div>
                  <span className="question-count">{quizForm.questions.length} total</span>
                </div>
                {quizForm.questions.map((question, questionIndex) => (
                  <fieldset className="builder-question" key={questionIndex}>
                    <div className="builder-question-heading">
                      <legend>Question {String(questionIndex + 1).padStart(2, '0')}</legend>
                      {quizForm.questions.length > 1 && (
                        <button
                          className="remove-question"
                          type="button"
                          onClick={() =>
                            setQuizForm({
                              ...quizForm,
                              questions: quizForm.questions.filter((_, index) => index !== questionIndex),
                            })
                          }
                        >
                          Remove
                        </button>
                      )}
                    </div>
                    <label className="form-label">
                      Question text
                      <input
                        required
                        maxLength={2000}
                        value={question.questionText}
                        onChange={(event) =>
                          updateQuestion(setQuizForm, quizForm, questionIndex, {
                            questionText: event.target.value,
                          })
                        }
                        placeholder="Write your question"
                      />
                    </label>
                    <div className="builder-options">
                      {question.options.map((option, optionIndex) => (
                        <label className="form-label option-input" key={optionIndex}>
                          <span>Option {String.fromCharCode(65 + optionIndex)}</span>
                          <input
                            required
                            maxLength={500}
                            value={option}
                            onChange={(event) => {
                              const options = [...question.options]
                              options[optionIndex] = event.target.value
                              updateQuestion(setQuizForm, quizForm, questionIndex, { options })
                            }}
                            placeholder={`Answer option ${optionIndex + 1}`}
                          />
                        </label>
                      ))}
                    </div>
                    <label className="form-label correct-picker">
                      Correct answer
                      <select
                        required
                        value={question.correctOption}
                        onChange={(event) =>
                          updateQuestion(setQuizForm, quizForm, questionIndex, {
                            correctOption: event.target.value,
                          })
                        }
                      >
                        <option value="">Choose the correct option</option>
                        {question.options.map((option, optionIndex) => (
                          <option key={optionIndex} value={optionIndex}>
                            {String.fromCharCode(65 + optionIndex)} — {option || `Option ${optionIndex + 1}`}
                          </option>
                        ))}
                      </select>
                    </label>
                  </fieldset>
                ))}
                <button
                  className="button button-secondary add-question-button"
                  type="button"
                  onClick={() => setQuizForm({ ...quizForm, questions: [...quizForm.questions, blankQuestion()] })}
                >
                  + Add another question
                </button>
              </div>
              {validationMessage && <p className="form-message" role="alert">{validationMessage}</p>}
              <div className="builder-submit-row">
                <p>Published quizzes are visible to every student using this demo.</p>
                <button className="button button-primary" type="submit" disabled={saving}>
                  {saving ? 'Publishing…' : 'Publish quiz'} {!saving && <span aria-hidden="true">→</span>}
                </button>
              </div>
            </form>
          </>
        )}

        {view === 'student' && (
          <>
            <section className="dashboard-heading">
              <div>
                <p className="eyebrow"><span className="eyebrow-line" />STUDENT DASHBOARD</p>
                <h1>Hi, {student.name || 'student'}.</h1>
                <p>Pick a published quiz and see what you know.</p>
              </div>
              <span className="student-id-pill">ID · {student.id}</span>
            </section>
            {error && <ErrorNotice message={error} onRetry={() => openStudentDashboard()} />}
            {loading ? (
              <LoadingCard label="Finding published quizzes…" />
            ) : availableQuizzes.length === 0 ? (
              <EmptyState title="No published quizzes yet" text="Check back when a teacher has published a quiz." />
            ) : (
              <section className="quiz-card-grid">
                {availableQuizzes.map((quiz) => (
                  <article className="platform-quiz-card student-quiz-card" key={quiz.id}>
                    <div className="quiz-card-top">
                      <span className="published-pill">READY TO TAKE</span>
                      <span className="question-count">{quiz.questionCount} questions</span>
                    </div>
                    <h2>{quiz.title}</h2>
                    <p>{quiz.description}</p>
                    <div className="quiz-card-actions">
                      <button className="button button-primary" type="button" onClick={() => openQuiz(quiz.id)}>
                        Take this quiz <span aria-hidden="true">→</span>
                      </button>
                    </div>
                  </article>
                ))}
              </section>
            )}
          </>
        )}

        {view === 'taking' && (
          <>
            {loading ? (
              <LoadingCard label="Loading your quiz…" />
            ) : error && !selectedQuiz ? (
              <ErrorNotice message={error} onRetry={() => openQuiz(selectedQuizId)} />
            ) : selectedQuiz && result ? (
              <section className="results-card platform-results" aria-live="polite">
                <div className="results-heading">
                  <span className="results-kicker">ATTEMPT RECORDED</span>
                  <h2>Nice work, {student.name}.</h2>
                  <p>{selectedQuiz.title} · Submitted {formatDate(result.submittedAt)}</p>
                </div>
                <div className="score-summary">
                  <div className="score-badge">
                    <strong>{result.percentage}%</strong><span>YOUR SCORE</span>
                  </div>
                  <div className="score-details">
                    <span className="score-fraction">{result.score}<span> / {result.totalQuestions}</span></span>
                    <span className="score-caption">marks earned</span>
                  </div>
                </div>
                <div className="answer-review">
                  <h3>Answer review</h3>
                  {result.correctAnswers.map((review, index) => (
                    <article
                      className={`review-item ${review.correct ? 'review-correct' : 'review-incorrect'}`}
                      key={review.questionId}
                    >
                      <div className="review-question">
                        <span className="review-number">{String(index + 1).padStart(2, '0')}</span>
                        <p>{review.questionText}</p>
                      </div>
                      <p className="review-answer">Your answer: <strong>{review.selectedAnswer}</strong></p>
                      {!review.correct && (
                        <p className="review-answer correct-answer">
                          Correct answer: <strong>{review.correctAnswer}</strong>
                        </p>
                      )}
                      {review.correct && <span className="correct-tag">✓ Correct</span>}
                    </article>
                  ))}
                </div>
                <div className="quiz-card-actions result-actions">
                  <button className="button button-primary" type="button" onClick={() => openQuiz(selectedQuiz.id)}>
                    Retake quiz <span aria-hidden="true">↻</span>
                  </button>
                  <button className="button button-secondary" type="button" onClick={() => openStudentDashboard()}>
                    Back to quizzes
                  </button>
                </div>
              </section>
            ) : selectedQuiz ? (
              <>
                <section className="dashboard-heading taking-heading">
                  <div>
                    <p className="eyebrow"><span className="eyebrow-line" />YOUR QUIZ</p>
                    <h1>{selectedQuiz.title}</h1>
                    <p>{selectedQuiz.description}</p>
                  </div>
                </section>
                <section className="quiz-progress" aria-label="Quiz progress">
                  <div className="progress-copy">
                    <span className="progress-label">YOUR PROGRESS</span>
                    <span className="progress-count"><strong>{answeredCount}</strong> of {questionCount} answered</span>
                  </div>
                  <div
                    className="progress-track"
                    role="progressbar"
                    aria-label="Questions answered"
                    aria-valuemin={0}
                    aria-valuemax={questionCount}
                    aria-valuenow={answeredCount}
                  >
                    <span className="progress-fill" style={{ width: `${progress}%` }} />
                  </div>
                </section>
                {error && <ErrorNotice message={error} />}
                <form onSubmit={handleQuizSubmit}>
                  <div className="question-list">
                    {selectedQuiz.questions.map((question, index) => (
                      <fieldset className="question-card" key={question.id} disabled={loading}>
                        <legend className="visually-hidden">Question {index + 1}: {question.questionText}</legend>
                        <div className="question-heading">
                          <span className="question-number"><span>QUESTION</span>{String(index + 1).padStart(2, '0')}</span>
                          <span className="question-type">MULTIPLE CHOICE</span>
                        </div>
                        <h2>{question.questionText}</h2>
                        <div className="option-list">
                          {question.options.map((option, optionIndex) => {
                            const isSelected = answers[question.id] === option
                            return (
                              <button
                                className={`option-button${isSelected ? ' option-selected' : ''}`}
                                type="button"
                                key={`${question.id}-${optionIndex}`}
                                aria-pressed={isSelected}
                                onClick={() =>
                                  setAnswers((current) => ({ ...current, [question.id]: option }))
                                }
                              >
                                <span className="option-letter">{String.fromCharCode(65 + optionIndex)}</span>
                                <span className="option-text">{option}</span>
                                {isSelected && <span className="option-check" aria-hidden="true">✓</span>}
                              </button>
                            )
                          })}
                        </div>
                      </fieldset>
                    ))}
                  </div>
                  {validationMessage && <p className="form-message" role="alert">{validationMessage}</p>}
                  <div className="submit-row">
                    <p>Your answers and correct answers appear after you submit.</p>
                    <button className="button button-primary submit-button" type="submit" disabled={loading}>
                      {loading ? 'Submitting…' : <>Submit quiz <span aria-hidden="true">→</span></>}
                    </button>
                  </div>
                </form>
              </>
            ) : null}
          </>
        )}
      </main>

      <footer className="footer">
        <span className="footer-brand">QuizBox</span>
        <span>Made for curious minds.</span>
      </footer>
    </div>
  )
}

function updateQuestion(setQuizForm, quizForm, questionIndex, changes) {
  setQuizForm({
    ...quizForm,
    questions: quizForm.questions.map((question, index) =>
      index === questionIndex ? { ...question, ...changes } : question,
    ),
  })
}

function LoadingCard({ label }) {
  return (
    <section className="status-card" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <h2>One moment</h2>
      <p>{label}</p>
    </section>
  )
}

function ErrorNotice({ message, onRetry }) {
  return (
    <div className="platform-error" role="alert">
      <span>{message}</span>
      {onRetry && <button className="text-button" type="button" onClick={onRetry}>Try again</button>}
    </div>
  )
}

function EmptyState({ title, text, action }) {
  return (
    <section className="status-card empty-state">
      <span className="status-icon" aria-hidden="true">✦</span>
      <h2>{title}</h2>
      <p>{text}</p>
      {action}
    </section>
  )
}

export default App
