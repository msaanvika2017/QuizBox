package com.quizbox.quizbox.dto;

import java.util.List;

public record QuizQuestionCreateRequest(String questionText, List<String> options, String correctAnswer) {
}
