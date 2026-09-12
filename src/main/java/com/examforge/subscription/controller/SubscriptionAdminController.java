package com.examforge.subscription.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.subscription.dto.GrantSubscriptionRequest;
import com.examforge.subscription.dto.SubscriptionResponse;
import com.examforge.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Admin - Subscriptions", description = "Admin-granted subscriptions (support/promo, bypasses payment)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class SubscriptionAdminController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/grant")
    @Operation(summary = "Manually activate a subscription for a user",
            description = "Bypasses payment entirely - for support or promotional grants. Supersedes any existing active subscription that user has.")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> grant(@Valid @RequestBody GrantSubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.adminGrant(request.getUserId(), request.getPlanId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subscription granted", response));
    }
}
