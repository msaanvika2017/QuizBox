package com.quizbox.quizbox.dto;

import java.util.List;

public record QuizCreateRequest(String title, String description, List<QuizQuestionCreateRequest> questions) {
}
