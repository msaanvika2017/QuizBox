package com.quizbox.quizbox.dto;

import java.time.LocalDateTime;

public record QuizSummaryDto(
		Long id,
		String title,
		String description,
		int questionCount,
		boolean published,
		LocalDateTime createdAt) {
}
