package com.examforge.plan.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.plan.dto.PlanResponse;
import com.examforge.plan.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Tag(name = "Plans", description = "Public subscription plan catalog")
public class PlanController {

    private final PlanService planService;

    @GetMapping
    @Operation(summary = "List active subscription plans")
    public ApiResponse<List<PlanResponse>> list() {
        return ApiResponse.success(planService.getActivePlans());
    }
}
