package com.quizbox.quizbox.service;

import com.quizbox.quizbox.dto.AnswerReviewDto;
import com.quizbox.quizbox.dto.AnswerSubmissionDto;
import com.quizbox.quizbox.dto.QuizQuestionDto;
import com.quizbox.quizbox.dto.QuizSubmissionRequest;
import com.quizbox.quizbox.dto.QuizSubmissionResultDto;
import com.quizbox.quizbox.entity.QuizQuestion;
import com.quizbox.quizbox.repository.QuizQuestionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizQuestionService {

	private final QuizQuestionRepository questionRepository;

	public QuizQuestionService(QuizQuestionRepository questionRepository) {
		this.questionRepository = questionRepository;
	}

	public List<QuizQuestionDto> getQuestions() {
		return questionRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
				.map(question -> new QuizQuestionDto(
						question.getId(), question.getQuestionText(), question.getOptions()))
				.toList();
	}

	public QuizSubmissionResultDto submitAnswers(QuizSubmissionRequest request) {
		if (request == null || request.answers() == null || request.answers().isEmpty()) {
			throw new IllegalArgumentException("Submit at least one answer.");
		}

		Set<Long> questionIds = new HashSet<>();
		for (AnswerSubmissionDto answer : request.answers()) {
			if (answer == null || answer.questionId() == null) {
				throw new IllegalArgumentException("Every answer must include a question ID.");
			}
			if (answer.selectedAnswer() == null || answer.selectedAnswer().isBlank()) {
				throw new IllegalArgumentException("Every answer must include a selected answer.");
			}
			if (!questionIds.add(answer.questionId())) {
				throw new IllegalArgumentException("A question can only be answered once.");
			}
		}

		Map<Long, QuizQuestion> questionsById = questionRepository.findAllById(questionIds).stream()
				.collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));

		List<AnswerReviewDto> review = new ArrayList<>();
		int score = 0;
		for (AnswerSubmissionDto answer : request.answers()) {
			QuizQuestion question = questionsById.get(answer.questionId());
			if (question == null) {
				throw new IllegalArgumentException("Question ID " + answer.questionId() + " does not exist.");
			}
			if (!question.getOptions().contains(answer.selectedAnswer())) {
				throw new IllegalArgumentException(
						"Selected answer is not a valid option for question ID " + answer.questionId() + ".");
			}

			boolean correct = question.getCorrectAnswer().equals(answer.selectedAnswer());
			if (correct) {
				score++;
			}
			review.add(new AnswerReviewDto(
					question.getId(),
					question.getQuestionText(),
					answer.selectedAnswer(),
					question.getCorrectAnswer(),
					correct));
		}

		int totalQuestions = review.size();
		double percentage = Math.round(score * 10000.0 / totalQuestions) / 100.0;
		return new QuizSubmissionResultDto(score, totalQuestions, percentage, List.copyOf(review));
	}
}
