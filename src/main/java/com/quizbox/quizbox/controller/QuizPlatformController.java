package com.quizbox.quizbox.controller;

import com.quizbox.quizbox.dto.QuizAttemptResultDto;
import com.quizbox.quizbox.dto.QuizAttemptSummaryDto;
import com.quizbox.quizbox.dto.QuizCreateRequest;
import com.quizbox.quizbox.dto.QuizDetailDto;
import com.quizbox.quizbox.dto.QuizSummaryDto;
import com.quizbox.quizbox.dto.StudentQuizSubmissionRequest;
import com.quizbox.quizbox.service.QuizPlatformService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class QuizPlatformController {

	private final QuizPlatformService quizPlatformService;

	public QuizPlatformController(QuizPlatformService quizPlatformService) {
		this.quizPlatformService = quizPlatformService;
	}

	@GetMapping
	public List<QuizSummaryDto> getTeacherQuizzes() {
		return quizPlatformService.getTeacherQuizzes();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public QuizSummaryDto createQuiz(@RequestBody QuizCreateRequest request) {
		return quizPlatformService.createQuiz(request);
	}

	@GetMapping("/available")
	public List<QuizSummaryDto> getAvailableQuizzes() {
		return quizPlatformService.getAvailableQuizzes();
	}

	@GetMapping("/{quizId}/questions")
	public QuizDetailDto getPublishedQuiz(@PathVariable Long quizId) {
		return quizPlatformService.getPublishedQuiz(quizId);
	}

	@PostMapping("/{quizId}/submit")
	public QuizAttemptResultDto submitQuiz(
			@PathVariable Long quizId,
			@RequestBody StudentQuizSubmissionRequest request) {
		return quizPlatformService.submitQuiz(quizId, request);
	}

	@GetMapping("/{quizId}/results")
	public List<QuizAttemptSummaryDto> getQuizResults(@PathVariable Long quizId) {
		return quizPlatformService.getQuizResults(quizId);
	}
}
