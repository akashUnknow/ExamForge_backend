package com.examforge.exam.dto;

import com.examforge.exam.domain.Exam;
import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.exam.domain.ExamStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExamResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        String name,
        String description,
        Integer durationMinutes,
        Integer totalQuestions,
        BigDecimal maximumMarks,
        BigDecimal negativeMarking,
        ExamDifficulty difficulty,
        ExamStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExamResponse from(Exam exam) {
        return new ExamResponse(
                exam.getId(),
                exam.getCategory() != null ? exam.getCategory().getId() : null,
                exam.getCategory() != null ? exam.getCategory().getName() : null,
                exam.getName(),
                exam.getDescription(),
                exam.getDurationMinutes(),
                exam.getTotalQuestions(),
                exam.getMaximumMarks(),
                exam.getNegativeMarking(),
                exam.getDifficulty(),
                exam.getStatus(),
                exam.getCreatedAt(),
                exam.getUpdatedAt()
        );
    }
}
