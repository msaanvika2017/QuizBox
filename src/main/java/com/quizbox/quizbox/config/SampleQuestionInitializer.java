package com.quizbox.quizbox.config;

import com.quizbox.quizbox.entity.QuizQuestion;
import com.quizbox.quizbox.entity.Quiz;
import com.quizbox.quizbox.entity.QuizItem;
import com.quizbox.quizbox.repository.QuizRepository;
import com.quizbox.quizbox.repository.QuizQuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SampleQuestionInitializer implements CommandLineRunner {

	private final QuizQuestionRepository questionRepository;
	private final QuizRepository quizRepository;

	public SampleQuestionInitializer(QuizQuestionRepository questionRepository, QuizRepository quizRepository) {
		this.questionRepository = questionRepository;
		this.quizRepository = quizRepository;
	}

	@Override
	public void run(String... args) {
		if (questionRepository.count() == 0) {
			questionRepository.saveAll(List.of(
					new QuizQuestion(
							"What is the capital of France?",
							"Berlin", "Madrid", "Paris", "Rome", "Paris"),
					new QuizQuestion(
							"What is 9 multiplied by 7?",
							"56", "63", "72", "81", "63"),
					new QuizQuestion(
							"Which planet is the largest in our solar system?",
							"Earth", "Jupiter", "Saturn", "Neptune", "Jupiter"),
					new QuizQuestion(
							"What does HTML stand for?",
							"HyperText Markup Language", "HighText Machine Language",
							"Hyperlink and Text Markup Language", "Home Tool Markup Language",
							"HyperText Markup Language"),
					new QuizQuestion(
							"What is the chemical formula for water?",
							"CO2", "O2", "H2O", "NaCl", "H2O")));
		}

		if (quizRepository.count() == 0) {
			Quiz starterQuiz = new Quiz("QuizBox Starter Quiz", "A starter quiz from your existing QuizBox questions.");
			questionRepository.findAll(org.springframework.data.domain.Sort.by("id")).forEach(question ->
					starterQuiz.addQuestion(new QuizItem(
							question.getQuestionText(),
							question.getOptions(),
							question.getCorrectAnswer())));
			if (!starterQuiz.getQuestions().isEmpty()) {
				quizRepository.save(starterQuiz);
			}
		}
	}
}
