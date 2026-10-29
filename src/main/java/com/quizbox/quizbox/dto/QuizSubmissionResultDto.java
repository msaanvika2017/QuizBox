package com.quizbox.quizbox.dto;

import java.util.List;

public record QuizSubmissionResultDto(
		int score,
		int totalQuestions,
		double percentage,
		List<AnswerReviewDto> correctAnswers) {
}
