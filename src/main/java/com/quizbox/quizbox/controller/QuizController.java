package com.quizbox.quizbox.controller;

import com.quizbox.quizbox.dto.QuizQuestionDto;
import com.quizbox.quizbox.dto.QuizSubmissionRequest;
import com.quizbox.quizbox.dto.QuizSubmissionResultDto;
import com.quizbox.quizbox.service.QuizQuestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

	private final QuizQuestionService quizQuestionService;

	public QuizController(QuizQuestionService quizQuestionService) {
		this.quizQuestionService = quizQuestionService;
	}

	@GetMapping("/questions")
	public List<QuizQuestionDto> getQuestions() {
		return quizQuestionService.getQuestions();
	}

	@PostMapping("/submit")
	public QuizSubmissionResultDto submitAnswers(@RequestBody QuizSubmissionRequest request) {
		return quizQuestionService.submitAnswers(request);
	}
}
