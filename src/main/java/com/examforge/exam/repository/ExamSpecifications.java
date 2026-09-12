package com.examforge.exam.repository;

import com.examforge.exam.domain.Exam;
import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.exam.domain.ExamStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Composable filter predicates for the exam catalog search. Each returns
 * null (no-op predicate) when the filter value is absent, so callers can
 * chain them unconditionally with Specification.where(...).and(...).
 */
public final class ExamSpecifications {

    private ExamSpecifications() {
    }

    public static Specification<Exam> hasStatus(ExamStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Exam> hasCategory(UUID categoryId) {
        return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Exam> hasDifficulty(ExamDifficulty difficulty) {
        return (root, query, cb) -> difficulty == null ? null : cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Exam> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%");
        };
    }
}
