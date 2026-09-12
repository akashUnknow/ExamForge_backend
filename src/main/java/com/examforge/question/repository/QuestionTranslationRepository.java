package com.examforge.question.repository;

import com.examforge.question.domain.QuestionTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionTranslationRepository extends JpaRepository<QuestionTranslation, UUID> {

    List<QuestionTranslation> findByQuestionId(UUID questionId);

    Optional<QuestionTranslation> findByQuestionIdAndLanguageIgnoreCase(UUID questionId, String language);

    boolean existsByQuestionIdAndLanguageIgnoreCase(UUID questionId, String language);
}
