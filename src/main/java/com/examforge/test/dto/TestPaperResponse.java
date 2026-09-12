package com.examforge.test.dto;

import com.examforge.test.domain.TestPaper;
import com.examforge.test.domain.TestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TestPaperResponse(
        UUID id,
        UUID examId,
        String examName,
        String name,
        String description,
        Integer durationMinutes,
        BigDecimal totalMarks,
        BigDecimal negativeMarking,
        TestStatus status,
        boolean free,
        BigDecimal price,
        Instant createdAt,
        Instant updatedAt
) {
    public static TestPaperResponse from(TestPaper test) {
        return new TestPaperResponse(
                test.getId(),
                test.getExam().getId(),
                test.getExam().getName(),
                test.getName(),
                test.getDescription(),
                test.getDurationMinutes(),
                test.getTotalMarks(),
                test.getNegativeMarking(),
                test.getStatus(),
                test.isFree(),
                test.getPrice(),
                test.getCreatedAt(),
                test.getUpdatedAt()
        );
    }
}
