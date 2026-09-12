package com.examforge.subscription.service;

import com.examforge.subscription.dto.SubscriptionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionService {

    Page<SubscriptionResponse> getMySubscriptions(Pageable pageable);

    Optional<SubscriptionResponse> getMyActiveSubscription();

    /** Self-service. FREE plans activate immediately; paid plans land PENDING until a future Payment module completes activation. */
    SubscriptionResponse subscribe(UUID planId);

    SubscriptionResponse cancelMySubscription(UUID subscriptionId);

    /** Admin-only bypass: activates immediately regardless of plan type or payment, for support/promo use. */
    SubscriptionResponse adminGrant(UUID userId, UUID planId);
}
