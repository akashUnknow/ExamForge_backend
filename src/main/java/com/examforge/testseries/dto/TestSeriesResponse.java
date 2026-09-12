package com.examforge.testseries.dto;

import com.examforge.test.domain.TestStatus;
import com.examforge.testseries.domain.TestSeries;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TestSeriesResponse(
        UUID id,
        UUID examId,
        String examName,
        String name,
        String description,
        boolean free,
        BigDecimal price,
        LocalDate startDate,
        LocalDate endDate,
        Integer accessDurationDays,
        TestStatus status
) {
    public static TestSeriesResponse from(TestSeries series) {
        return new TestSeriesResponse(
                series.getId(),
                series.getExam().getId(),
                series.getExam().getName(),
                series.getName(),
                series.getDescription(),
                series.isFree(),
                series.getPrice(),
                series.getStartDate(),
                series.getEndDate(),
                series.getAccessDurationDays(),
                series.getStatus()
        );
    }
}
