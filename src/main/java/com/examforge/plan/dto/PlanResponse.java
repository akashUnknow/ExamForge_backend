package com.examforge.plan.dto;

import com.examforge.plan.domain.Plan;
import com.examforge.plan.domain.PlanType;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        String name,
        PlanType planType,
        BigDecimal price,
        Integer durationDays,
        String description,
        boolean active
) {
    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getId(), plan.getName(), plan.getPlanType(), plan.getPrice(),
                plan.getDurationDays(), plan.getDescription(), plan.isActive());
    }
}
