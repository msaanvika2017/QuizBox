package com.quizbox.quizbox.dto;

public record AnswerReviewDto(
		Long questionId,
		String questionText,
		String selectedAnswer,
		String correctAnswer,
		boolean correct) {
}
