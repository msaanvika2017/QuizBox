package com.quizbox.quizbox.dto;

import java.time.LocalDateTime;

public record QuizAttemptSummaryDto(
		Long attemptId,
		Long quizId,
		String quizTitle,
		String studentName,
		String studentId,
		int score,
		int totalQuestions,
		double percentage,
		LocalDateTime submittedAt) {
}
