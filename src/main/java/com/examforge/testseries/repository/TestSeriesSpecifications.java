package com.examforge.testseries.repository;

import com.examforge.exam.domain.ExamStatus;
import com.examforge.test.domain.TestStatus;
import com.examforge.testseries.domain.TestSeries;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class TestSeriesSpecifications {

    private TestSeriesSpecifications() {
    }

    public static Specification<TestSeries> hasStatus(TestStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<TestSeries> hasExam(UUID examId) {
        return (root, query, cb) -> examId == null ? null : cb.equal(root.get("exam").get("id"), examId);
    }

    public static Specification<TestSeries> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%");
        };
    }

    /** Public visibility additionally requires the parent exam to be PUBLISHED. */
    public static Specification<TestSeries> examIsPublished() {
        return (root, query, cb) -> cb.equal(root.get("exam").get("status"), ExamStatus.PUBLISHED);
    }
}
