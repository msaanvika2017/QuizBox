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

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String studentName;

	@Column(nullable = false, length = 80)
	private String studentId;

	@Column(nullable = false)
	private int score;

	@Column(nullable = false)
	private int totalQuestions;

	@Column(nullable = false)
	private double percentage;

	@Column(nullable = false)
	private LocalDateTime submittedAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_id", nullable = false)
	private Quiz quiz;

	protected QuizAttempt() {
	}

	public QuizAttempt(String studentName, String studentId, int score, int totalQuestions,
			double percentage, Quiz quiz) {
		this.studentName = studentName;
		this.studentId = studentId;
		this.score = score;
		this.totalQuestions = totalQuestions;
		this.percentage = percentage;
		this.quiz = quiz;
		this.submittedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getStudentName() {
		return studentName;
	}

	public String getStudentId() {
		return studentId;
	}

	public int getScore() {
		return score;
	}

	public int getTotalQuestions() {
		return totalQuestions;
	}

	public double getPercentage() {
		return percentage;
	}

	public LocalDateTime getSubmittedAt() {
		return submittedAt;
	}

	public Quiz getQuiz() {
		return quiz;
	}
}
