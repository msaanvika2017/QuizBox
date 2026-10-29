package com.quizbox.quizbox.repository;

import com.quizbox.quizbox.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
	List<Quiz> findAllByOrderByCreatedAtDesc();

	List<Quiz> findAllByPublishedTrueOrderByCreatedAtDesc();
}
