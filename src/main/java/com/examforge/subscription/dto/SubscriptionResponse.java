package com.examforge.subscription.dto;

import com.examforge.subscription.domain.Subscription;
import com.examforge.subscription.domain.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID planId,
        String planName,
        SubscriptionStatus status,
        Instant startDate,
        Instant endDate,
        boolean currentlyActive
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getPlan().getId(),
                subscription.getPlan().getName(),
                subscription.getStatus(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.isCurrentlyActive()
        );
    }
}
