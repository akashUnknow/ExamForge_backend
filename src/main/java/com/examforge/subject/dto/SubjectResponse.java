package com.examforge.subject.dto;

import com.examforge.subject.domain.Subject;

import java.time.Instant;
import java.util.UUID;

public record SubjectResponse(
        UUID id,
        UUID examId,
        String name,
        String description,
        Integer displayOrder,
        Instant createdAt
) {
    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(
                subject.getId(),
                subject.getExam().getId(),
                subject.getName(),
                subject.getDescription(),
                subject.getDisplayOrder(),
                subject.getCreatedAt()
        );
    }
}
