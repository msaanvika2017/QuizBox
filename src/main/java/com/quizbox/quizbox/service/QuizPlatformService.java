package com.quizbox.quizbox.service;

import com.quizbox.quizbox.dto.AnswerReviewDto;
import com.quizbox.quizbox.dto.AnswerSubmissionDto;
import com.quizbox.quizbox.dto.QuizAttemptResultDto;
import com.quizbox.quizbox.dto.QuizAttemptSummaryDto;
import com.quizbox.quizbox.dto.QuizCreateRequest;
import com.quizbox.quizbox.dto.QuizDetailDto;
import com.quizbox.quizbox.dto.QuizQuestionCreateRequest;
import com.quizbox.quizbox.dto.QuizQuestionDto;
import com.quizbox.quizbox.dto.QuizSummaryDto;
import com.quizbox.quizbox.dto.StudentQuizSubmissionRequest;
import com.quizbox.quizbox.entity.Quiz;
import com.quizbox.quizbox.entity.QuizAttempt;
import com.quizbox.quizbox.entity.QuizItem;
import com.quizbox.quizbox.repository.QuizAttemptRepository;
import com.quizbox.quizbox.repository.QuizRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizPlatformService {

	private final QuizRepository quizRepository;
	private final QuizAttemptRepository attemptRepository;

	public QuizPlatformService(QuizRepository quizRepository, QuizAttemptRepository attemptRepository) {
		this.quizRepository = quizRepository;
		this.attemptRepository = attemptRepository;
	}

	@Transactional
	public QuizSummaryDto createQuiz(QuizCreateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Quiz details are required.");
		}
		String title = requiredText(request.title(), "Quiz title", 160);
		String description = requiredText(request.description(), "Quiz description", 2000);
		if (request.questions() == null || request.questions().isEmpty()) {
			throw new IllegalArgumentException("Add at least one question before publishing.");
		}

		Quiz quiz = new Quiz(title, description);
		for (QuizQuestionCreateRequest question : request.questions()) {
			validateQuestion(question);
			List<String> options = question.options().stream().map(String::trim).toList();
			String correctAnswer = question.correctAnswer().trim();
			if (!options.contains(correctAnswer)) {
				throw new IllegalArgumentException("The correct answer must match one of the four options.");
			}
			quiz.addQuestion(new QuizItem(
					requiredText(question.questionText(), "Question text", 2000),
					options,
					correctAnswer));
		}

		return toSummary(quizRepository.save(quiz));
	}

	@Transactional
	public List<QuizSummaryDto> getTeacherQuizzes() {
		return quizRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toSummary).toList();
	}

	@Transactional
	public List<QuizSummaryDto> getAvailableQuizzes() {
		return quizRepository.findAllByPublishedTrueOrderByCreatedAtDesc().stream().map(this::toSummary).toList();
	}

	@Transactional
	public QuizDetailDto getPublishedQuiz(Long quizId) {
		Quiz quiz = findQuiz(quizId);
		if (!quiz.isPublished()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found.");
		}
		return toDetail(quiz);
	}

	@Transactional
	public QuizAttemptResultDto submitQuiz(Long quizId, StudentQuizSubmissionRequest request) {
		Quiz quiz = findQuiz(quizId);
		if (!quiz.isPublished()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found.");
		}
		if (request == null) {
			throw new IllegalArgumentException("Student details and answers are required.");
		}
		String studentName = requiredText(request.studentName(), "Student name", 120);
		String studentId = requiredText(request.studentId(), "Student ID", 80);
		List<AnswerSubmissionDto> answers = request.answers();
		if (answers == null || answers.isEmpty()) {
			throw new IllegalArgumentException("Submit an answer for every question.");
		}

		Map<Long, QuizItem> questionsById = quiz.getQuestions().stream()
				.collect(Collectors.toMap(QuizItem::getId, Function.identity()));
		Map<Long, String> selectedAnswers = new java.util.HashMap<>();
		Set<Long> answeredIds = new HashSet<>();
		for (AnswerSubmissionDto answer : answers) {
			if (answer == null || answer.questionId() == null) {
				throw new IllegalArgumentException("Every answer must include a question ID.");
			}
			if (!answeredIds.add(answer.questionId())) {
				throw new IllegalArgumentException("A question can only be answered once.");
			}
			QuizItem question = questionsById.get(answer.questionId());
			if (question == null) {
				throw new IllegalArgumentException("An answer refers to a question outside this quiz.");
			}
			if (answer.selectedAnswer() == null || !question.getOptions().contains(answer.selectedAnswer())) {
				throw new IllegalArgumentException("Every selected answer must be a valid option.");
			}
			selectedAnswers.put(answer.questionId(), answer.selectedAnswer());
		}
		if (answeredIds.size() != questionsById.size()) {
			throw new IllegalArgumentException("Submit an answer for every question.");
		}

		List<AnswerReviewDto> review = new ArrayList<>();
		int score = 0;
		for (QuizItem question : quiz.getQuestions()) {
			String selectedAnswer = selectedAnswers.get(question.getId());
			boolean correct = question.getCorrectAnswer().equals(selectedAnswer);
			if (correct) {
				score++;
			}
			review.add(new AnswerReviewDto(
					question.getId(),
					question.getQuestionText(),
					selectedAnswer,
					question.getCorrectAnswer(),
					correct));
		}

		int totalQuestions = quiz.getQuestions().size();
		double percentage = Math.round(score * 10000.0 / totalQuestions) / 100.0;
		QuizAttempt attempt = attemptRepository.save(new QuizAttempt(
				studentName, studentId, score, totalQuestions, percentage, quiz));
		return new QuizAttemptResultDto(
				attempt.getId(),
				quiz.getTitle(),
				score,
				totalQuestions,
				percentage,
				attempt.getSubmittedAt(),
				List.copyOf(review));
	}

	@Transactional
	public List<QuizAttemptSummaryDto> getQuizResults(Long quizId) {
		Quiz quiz = findQuiz(quizId);
		return attemptRepository.findAllByQuizIdOrderBySubmittedAtDesc(quiz.getId()).stream()
				.map(attempt -> new QuizAttemptSummaryDto(
						attempt.getId(),
						quiz.getId(),
						quiz.getTitle(),
						attempt.getStudentName(),
						attempt.getStudentId(),
						attempt.getScore(),
						attempt.getTotalQuestions(),
						attempt.getPercentage(),
						attempt.getSubmittedAt()))
				.toList();
	}

	private Quiz findQuiz(Long quizId) {
		if (quizId == null) {
			throw new IllegalArgumentException("Quiz ID is required.");
		}
		return quizRepository.findById(quizId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));
	}

	private QuizSummaryDto toSummary(Quiz quiz) {
		return new QuizSummaryDto(
				quiz.getId(),
				quiz.getTitle(),
				quiz.getDescription(),
				quiz.getQuestions().size(),
				quiz.isPublished(),
				quiz.getCreatedAt());
	}

	private QuizDetailDto toDetail(Quiz quiz) {
		List<QuizQuestionDto> questions = quiz.getQuestions().stream()
				.map(question -> new QuizQuestionDto(
						question.getId(), question.getQuestionText(), question.getOptions()))
				.toList();
		return new QuizDetailDto(
				quiz.getId(),
				quiz.getTitle(),
				quiz.getDescription(),
				quiz.isPublished(),
				quiz.getCreatedAt(),
				questions);
	}

	private void validateQuestion(QuizQuestionCreateRequest question) {
		if (question == null) {
			throw new IllegalArgumentException("Every question must include its text, options, and correct answer.");
		}
		requiredText(question.questionText(), "Question text", 2000);
		if (question.options() == null || question.options().size() != 4) {
			throw new IllegalArgumentException("Every question must have exactly four options.");
		}
		List<String> options = question.options().stream()
				.map(option -> requiredText(option, "Answer option", 500))
				.toList();
		if (new HashSet<>(options).size() != 4) {
			throw new IllegalArgumentException("Each question must have four different options.");
		}
		requiredText(question.correctAnswer(), "Correct answer", 500);
	}

	private String requiredText(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " is required.");
		}
		String trimmed = value.trim();
		if (trimmed.length() > maxLength) {
			throw new IllegalArgumentException(field + " must be no longer than " + maxLength + " characters.");
		}
		return trimmed;
	}
}
