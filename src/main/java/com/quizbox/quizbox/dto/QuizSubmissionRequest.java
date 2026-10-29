package com.quizbox.quizbox.dto;

import java.util.List;

public record QuizSubmissionRequest(List<AnswerSubmissionDto> answers) {
}
