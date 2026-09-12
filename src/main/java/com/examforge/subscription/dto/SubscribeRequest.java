package com.examforge.subscription.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class SubscribeRequest {

    @NotNull(message = "Plan id is required")
    private UUID planId;

    public UUID getPlanId() {
        return planId;
    }

    public void setPlanId(UUID planId) {
        this.planId = planId;
    }
}
