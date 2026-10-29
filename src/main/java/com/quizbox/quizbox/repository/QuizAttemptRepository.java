package com.quizbox.quizbox.repository;

import com.quizbox.quizbox.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
	List<QuizAttempt> findAllByQuizIdOrderBySubmittedAtDesc(Long quizId);
}
