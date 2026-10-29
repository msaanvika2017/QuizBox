package com.quizbox.quizbox;

import com.quizbox.quizbox.dto.AnswerSubmissionDto;
import com.quizbox.quizbox.dto.QuizCreateRequest;
import com.quizbox.quizbox.dto.QuizQuestionCreateRequest;
import com.quizbox.quizbox.dto.QuizSubmissionRequest;
import com.quizbox.quizbox.dto.QuizSubmissionResultDto;
import com.quizbox.quizbox.dto.StudentQuizSubmissionRequest;
import com.quizbox.quizbox.entity.QuizQuestion;
import com.quizbox.quizbox.repository.QuizQuestionRepository;
import com.quizbox.quizbox.service.QuizPlatformService;
import com.quizbox.quizbox.service.QuizQuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QuizboxApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private QuizQuestionRepository questionRepository;

	@Autowired
	private QuizQuestionService quizQuestionService;

	@Autowired
	private QuizPlatformService quizPlatformService;

	@Test
	void questionsEndpointReturnsQuestionsWithoutCorrectAnswers() throws Exception {
		mockMvc.perform(get("/api/quiz/questions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(5))
				.andExpect(jsonPath("$[0].id").exists())
				.andExpect(jsonPath("$[0].questionText").exists())
				.andExpect(jsonPath("$[0].options.length()").value(4))
				.andExpect(jsonPath("$[0].correctAnswer").doesNotExist());
	}

	@Test
	void submissionCalculatesScoreAndReturnsAnswersForReview() {
		List<QuizQuestion> questions = questionRepository.findAll();
		QuizQuestion firstQuestion = questions.get(0);
		QuizQuestion secondQuestion = questions.get(1);
		String incorrectAnswer = secondQuestion.getOptions().stream()
				.filter(option -> !option.equals(secondQuestion.getCorrectAnswer()))
				.findFirst()
				.orElseThrow();

		QuizSubmissionRequest request = new QuizSubmissionRequest(List.of(
				new AnswerSubmissionDto(firstQuestion.getId(), firstQuestion.getCorrectAnswer()),
				new AnswerSubmissionDto(secondQuestion.getId(), incorrectAnswer)));

		QuizSubmissionResultDto result = quizQuestionService.submitAnswers(request);

		assertEquals(1, result.score());
		assertEquals(2, result.totalQuestions());
		assertEquals(50.0, result.percentage());
		assertEquals(2, result.correctAnswers().size());
		assertTrue(result.correctAnswers().get(0).correct());
		assertEquals(secondQuestion.getCorrectAnswer(), result.correctAnswers().get(1).correctAnswer());
	}

	@Test
	void teacherCanPublishQuizAndStudentAttemptIsRecordedForTeacherResults() throws Exception {
		String quizJson = """
				{
				  "title": "Java Basics",
				  "description": "A short Java quiz.",
				  "questions": [{
				    "questionText": "Which keyword creates a class?",
				    "options": ["class", "method", "package", "return"],
				    "correctAnswer": "class"
				  }]
				}
				""";

		String createdJson = mockMvc.perform(post("/api/quizzes")
						.contentType("application/json")
						.content(quizJson))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Java Basics"))
				.andReturn().getResponse().getContentAsString();
		long quizId = firstId(createdJson);

		String questionsJson = mockMvc.perform(get("/api/quizzes/{quizId}/questions", quizId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.questions.length()").value(1))
				.andExpect(jsonPath("$.questions[0].correctAnswer").doesNotExist())
				.andReturn().getResponse().getContentAsString();
		long questionId = firstId(questionsJson.substring(questionsJson.indexOf("\"questions\"")));

		mockMvc.perform(post("/api/quizzes/{quizId}/submit", quizId)
						.contentType("application/json")
						.content("""
								{
								  "studentName": "Alex Student",
								  "studentId": "S123",
								  "answers": [{"questionId": %d, "selectedAnswer": "class"}]
								}
								""".formatted(questionId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.score").value(1))
				.andExpect(jsonPath("$.correctAnswers[0].correctAnswer").value("class"));

		mockMvc.perform(get("/api/quizzes/{quizId}/results", quizId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].studentName").value("Alex Student"))
				.andExpect(jsonPath("$[0].studentId").value("S123"))
				.andExpect(jsonPath("$[0].quizTitle").value("Java Basics"))
				.andExpect(jsonPath("$[0].percentage").value(100.0))
				.andExpect(jsonPath("$[0].submittedAt").exists());
	}

	@Test
	void quizSubmissionRejectsDuplicateAndOutOfQuizQuestionIds() {
		var quiz = quizPlatformService.createQuiz(new QuizCreateRequest(
				"Validation quiz", "Checks answer validation.", List.of(
						new QuizQuestionCreateRequest("Pick one.", List.of("One", "Two", "Three", "Four"), "One"))));
		var otherQuiz = quizPlatformService.createQuiz(new QuizCreateRequest(
				"Other quiz", "Another question.", List.of(
						new QuizQuestionCreateRequest("Pick two.", List.of("A", "B", "C", "D"), "A"))));
		Long questionId = quizPlatformService.getPublishedQuiz(quiz.id()).questions().get(0).id();
		Long foreignQuestionId = quizPlatformService.getPublishedQuiz(otherQuiz.id()).questions().get(0).id();
		var duplicateAnswers = new StudentQuizSubmissionRequest(
				"Alex", "S123", List.of(
						new AnswerSubmissionDto(questionId, "One"),
						new AnswerSubmissionDto(questionId, "Two")));
		var foreignAnswer = new StudentQuizSubmissionRequest(
				"Alex", "S123", List.of(new AnswerSubmissionDto(foreignQuestionId, "A")));

		assertThrows(
				IllegalArgumentException.class,
				() -> quizPlatformService.submitQuiz(quiz.id(), duplicateAnswers));
		assertThrows(
				IllegalArgumentException.class,
				() -> quizPlatformService.submitQuiz(quiz.id(), foreignAnswer));
	}

	private long firstId(String json) {
		Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(json);
		assertTrue(matcher.find(), "Expected an id in the API response.");
		return Long.parseLong(matcher.group(1));
	}
}
