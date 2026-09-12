package com.examforge.subscription.controller;

import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.subscription.dto.SubscribeRequest;
import com.examforge.subscription.dto.SubscriptionResponse;
import com.examforge.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Self-service subscription management")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/me")
    @Operation(summary = "The current user's subscription history")
    public ApiResponse<PageResponse<SubscriptionResponse>> myHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(subscriptionService.getMySubscriptions(pageable)));
    }

    @GetMapping("/me/active")
    @Operation(summary = "The current user's active subscription, if any")
    public ApiResponse<SubscriptionResponse> myActive() {
        SubscriptionResponse response = subscriptionService.getMyActiveSubscription()
                .orElseThrow(() -> ResourceNotFoundException.of("Active subscription", "current user"));
        return ApiResponse.success(response);
    }

    @PostMapping
    @Operation(summary = "Subscribe to a plan",
            description = "FREE plans activate immediately. Paid plans are created PENDING - payment integration is a later phase.")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribe(@Valid @RequestBody SubscribeRequest request) {
        SubscriptionResponse response = subscriptionService.subscribe(request.getPlanId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subscription created", response));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel the current user's own active subscription")
    public ApiResponse<SubscriptionResponse> cancel(@PathVariable UUID id) {
        return ApiResponse.success("Subscription cancelled", subscriptionService.cancelMySubscription(id));
    }
}
