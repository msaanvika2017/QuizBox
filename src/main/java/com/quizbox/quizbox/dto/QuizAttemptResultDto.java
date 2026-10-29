package com.quizbox.quizbox.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuizAttemptResultDto(
		Long attemptId,
		String quizTitle,
		int score,
		int totalQuestions,
		double percentage,
		LocalDateTime submittedAt,
		List<AnswerReviewDto> correctAnswers) {
}
