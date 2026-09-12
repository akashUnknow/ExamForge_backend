package com.examforge.plan.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.plan.dto.PlanRequest;
import com.examforge.plan.dto.PlanResponse;
import com.examforge.plan.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/plans")
@RequiredArgsConstructor
@Tag(name = "Admin - Plans", description = "Admin subscription plan management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class PlanAdminController {

    private final PlanService planService;

    @GetMapping
    @Operation(summary = "List all plans, including inactive ones")
    public ApiResponse<List<PlanResponse>> list() {
        return ApiResponse.success(planService.getAllPlans());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a plan by id")
    public ApiResponse<PlanResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(planService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a plan")
    public ResponseEntity<ApiResponse<PlanResponse>> create(@Valid @RequestBody PlanRequest request) {
        PlanResponse response = planService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Plan created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a plan")
    public ApiResponse<PlanResponse> update(@PathVariable UUID id, @Valid @RequestBody PlanRequest request) {
        return ApiResponse.success("Plan updated successfully", planService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a plan (blocked if subscriptions reference it)")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        planService.delete(id);
        return ApiResponse.success("Plan deleted successfully", null);
    }
}
