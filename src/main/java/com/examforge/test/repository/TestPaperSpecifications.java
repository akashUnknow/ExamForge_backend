package com.examforge.test.repository;

import com.examforge.test.domain.TestPaper;
import com.examforge.test.domain.TestStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class TestPaperSpecifications {

    private TestPaperSpecifications() {
    }

    public static Specification<TestPaper> hasStatus(TestStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<TestPaper> hasExam(UUID examId) {
        return (root, query, cb) -> examId == null ? null : cb.equal(root.get("exam").get("id"), examId);
    }

    public static Specification<TestPaper> isFree(Boolean free) {
        return (root, query, cb) -> free == null ? null : cb.equal(root.get("free"), free);
    }

    public static Specification<TestPaper> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%");
        };
    }

    /** Public visibility additionally requires the parent exam to be PUBLISHED. */
    public static Specification<TestPaper> examIsPublished() {
        return (root, query, cb) -> cb.equal(root.get("exam").get("status"),
                com.examforge.exam.domain.ExamStatus.PUBLISHED);
    }
}
