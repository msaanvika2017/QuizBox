package com.quizbox.quizbox.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(nullable = false, length = 2000)
	private String description;

	@Column(nullable = false)
	private boolean published;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<QuizItem> questions = new ArrayList<>();

	protected Quiz() {
	}

	public Quiz(String title, String description) {
		this.title = title;
		this.description = description;
		this.published = true;
		this.createdAt = LocalDateTime.now();
	}

	public void addQuestion(QuizItem question) {
		questions.add(question);
		question.setQuiz(this);
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public boolean isPublished() {
		return published;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public List<QuizItem> getQuestions() {
		return questions;
	}
}
