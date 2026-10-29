package com.quizbox.quizbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "quiz_items")
public class QuizItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 2000)
	private String questionText;

	@Column(nullable = false, length = 500)
	private String optionA;

	@Column(nullable = false, length = 500)
	private String optionB;

	@Column(nullable = false, length = 500)
	private String optionC;

	@Column(nullable = false, length = 500)
	private String optionD;

	@Column(nullable = false, length = 500)
	private String correctAnswer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_id", nullable = false)
	private Quiz quiz;

	protected QuizItem() {
	}

	public QuizItem(String questionText, List<String> options, String correctAnswer) {
		this.questionText = questionText;
		this.optionA = options.get(0);
		this.optionB = options.get(1);
		this.optionC = options.get(2);
		this.optionD = options.get(3);
		this.correctAnswer = correctAnswer;
	}

	void setQuiz(Quiz quiz) {
		this.quiz = quiz;
	}

	public Long getId() {
		return id;
	}

	public String getQuestionText() {
		return questionText;
	}

	public List<String> getOptions() {
		return List.of(optionA, optionB, optionC, optionD);
	}

	public String getCorrectAnswer() {
		return correctAnswer;
	}
}
