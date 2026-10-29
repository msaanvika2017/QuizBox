package com.quizbox.quizbox.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuizDetailDto(
		Long id,
		String title,
		String description,
		boolean published,
		LocalDateTime createdAt,
		List<QuizQuestionDto> questions) {
}
