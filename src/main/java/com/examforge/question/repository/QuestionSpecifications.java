package com.examforge.question.repository;

import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.question.domain.Question;
import com.examforge.question.domain.QuestionStatus;
import com.examforge.question.domain.QuestionType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class QuestionSpecifications {

    private QuestionSpecifications() {
    }

    public static Specification<Question> hasTopic(UUID topicId) {
        return (root, query, cb) -> topicId == null ? null : cb.equal(root.get("topic").get("id"), topicId);
    }

    public static Specification<Question> hasSubject(UUID subjectId) {
        return (root, query, cb) -> subjectId == null ? null
                : cb.equal(root.get("topic").get("subject").get("id"), subjectId);
    }

    public static Specification<Question> hasStatus(QuestionStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Question> hasDifficulty(ExamDifficulty difficulty) {
        return (root, query, cb) -> difficulty == null ? null : cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Question> hasType(QuestionType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("questionType"), type);
    }

    public static Specification<Question> createdByUser(String createdBy) {
        return (root, query, cb) -> createdBy == null ? null : cb.equal(root.get("createdBy"), createdBy);
    }

    public static Specification<Question> textContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("questionText")), "%" + keyword.trim().toLowerCase() + "%");
        };
    }
}
