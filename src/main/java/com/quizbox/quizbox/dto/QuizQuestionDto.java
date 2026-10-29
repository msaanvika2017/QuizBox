package com.quizbox.quizbox.dto;

import java.util.List;

public record QuizQuestionDto(Long id, String questionText, List<String> options) {
}
