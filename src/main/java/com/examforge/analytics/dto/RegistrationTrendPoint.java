package com.examforge.analytics.dto;

import java.time.LocalDate;

public record RegistrationTrendPoint(
        LocalDate date,
        long count
) {
}
