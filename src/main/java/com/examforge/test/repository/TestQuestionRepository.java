package com.examforge.test.repository;

import com.examforge.test.domain.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TestQuestionRepository extends JpaRepository<TestQuestion, UUID> {

    List<TestQuestion> findByTestIdOrderByDisplayOrderAsc(UUID testId);

    boolean existsByTestIdAndQuestionId(UUID testId, UUID questionId);

    boolean existsBySectionId(UUID sectionId);

    long countByTestId(UUID testId);

    /** Question ids already attached to this test - used by auto-generation to avoid duplicates. */
    @org.springframework.data.jpa.repository.Query(
            "select tq.question.id from TestQuestion tq where tq.test.id = :testId")
    Set<UUID> findQuestionIdsByTestId(UUID testId);
}
