package com.examforge.subscription.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class GrantSubscriptionRequest {

    @NotNull(message = "User id is required")
    private UUID userId;

    @NotNull(message = "Plan id is required")
    private UUID planId;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getPlanId() {
        return planId;
    }

    public void setPlanId(UUID planId) {
        this.planId = planId;
    }
}
