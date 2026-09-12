package com.examforge.exam.dto;

import com.examforge.exam.domain.ExamCategory;

import java.time.Instant;
import java.util.UUID;

public record ExamCategoryResponse(
        UUID id,
        String name,
        String description,
        Instant createdAt
) {
    public static ExamCategoryResponse from(ExamCategory category) {
        return new ExamCategoryResponse(
                category.getId(), category.getName(), category.getDescription(), category.getCreatedAt());
    }
}
