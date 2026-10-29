package com.quizbox.quizbox.dto;

import java.util.List;

public record StudentQuizSubmissionRequest(
		String studentName,
		String studentId,
		List<AnswerSubmissionDto> answers) {
}
