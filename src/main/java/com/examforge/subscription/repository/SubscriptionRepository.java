package com.examforge.subscription.repository;

import com.examforge.subscription.domain.Subscription;
import com.examforge.subscription.domain.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Page<Subscription> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Subscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    boolean existsByPlanId(UUID planId);

    /** True if the user has an ACTIVE subscription that hasn't expired yet - the actual access gate query. */
    @Query("select case when count(s) > 0 then true else false end from Subscription s " +
            "where s.user.id = :userId and s.status = 'ACTIVE' and (s.endDate is null or s.endDate > CURRENT_TIMESTAMP)")
    boolean hasActiveSubscription(@Param("userId") UUID userId);
}
