package com.examforge.subscription.service;

import com.examforge.common.exception.BusinessException;
import com.examforge.common.exception.ForbiddenException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.common.security.SecurityUtils;
import com.examforge.plan.domain.Plan;
import com.examforge.plan.domain.PlanType;
import com.examforge.plan.repository.PlanRepository;
import com.examforge.subscription.domain.Subscription;
import com.examforge.subscription.domain.SubscriptionStatus;
import com.examforge.subscription.dto.SubscriptionResponse;
import com.examforge.subscription.repository.SubscriptionRepository;
import com.examforge.user.domain.User;
import com.examforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getMySubscriptions(Pageable pageable) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(currentUserId(), pageable)
                .map(SubscriptionResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionResponse> getMyActiveSubscription() {
        return subscriptionRepository.findByUserIdAndStatus(currentUserId(), SubscriptionStatus.ACTIVE)
                .filter(Subscription::isCurrentlyActive)
                .map(SubscriptionResponse::from);
    }

    @Override
    @Transactional
    public SubscriptionResponse subscribe(UUID planId) {
        UUID userId = currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> ResourceNotFoundException.of("Plan", planId));

        if (!plan.isActive()) {
            throw new BusinessException("This plan is not currently available", HttpStatus.BAD_REQUEST);
        }
        if (subscriptionRepository.hasActiveSubscription(userId)) {
            throw new BusinessException("You already have an active subscription", HttpStatus.CONFLICT);
        }

        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStartDate(Instant.now());

        if (plan.getPlanType() == PlanType.FREE) {
            // No payment needed - activates immediately.
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setEndDate(computeEndDate(plan));
        } else {
            // Payment integration doesn't exist yet (a later phase) - this
            // deliberately does NOT auto-activate a paid plan. It stays
            // PENDING until a real payment flow completes it, or an admin
            // grants it directly via adminGrant().
            subscription.setStatus(SubscriptionStatus.PENDING);
        }

        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional
    public SubscriptionResponse cancelMySubscription(UUID subscriptionId) {
        UUID userId = currentUserId();
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Subscription", subscriptionId));

        if (!subscription.getUser().getId().equals(userId)) {
            throw ResourceNotFoundException.of("Subscription", subscriptionId);
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException("Only an active subscription can be cancelled", HttpStatus.BAD_REQUEST);
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setEndDate(Instant.now());

        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional
    public SubscriptionResponse adminGrant(UUID userId, UUID planId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> ResourceNotFoundException.of("Plan", planId));

        // Superseding an existing active subscription rather than blocking,
        // since an admin grant is meant to be authoritative (support/promo
        // override) - and this also avoids the DB's one-active-per-user
        // unique index rejecting the insert.
        subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE).ifPresent(existing -> {
            existing.setStatus(SubscriptionStatus.CANCELLED);
            existing.setEndDate(Instant.now());
            subscriptionRepository.save(existing);
        });

        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(Instant.now());
        subscription.setEndDate(computeEndDate(plan));

        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    private Instant computeEndDate(Plan plan) {
        return plan.getDurationDays() != null ? Instant.now().plus(Duration.ofDays(plan.getDurationDays())) : null;
    }

    private UUID currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new ForbiddenException("Authentication is required"));
    }
}
