package com.examforge.test.dto;

import com.examforge.test.domain.TestSection;

import java.math.BigDecimal;
import java.util.UUID;

public record TestSectionResponse(
        UUID id,
        UUID testId,
        String name,
        Integer durationMinutes,
        Integer questionCount,
        BigDecimal marks,
        Integer displayOrder
) {
    public static TestSectionResponse from(TestSection section) {
        return new TestSectionResponse(
                section.getId(),
                section.getTest().getId(),
                section.getName(),
                section.getDurationMinutes(),
                section.getQuestionCount(),
                section.getMarks(),
                section.getDisplayOrder()
        );
    }
}
