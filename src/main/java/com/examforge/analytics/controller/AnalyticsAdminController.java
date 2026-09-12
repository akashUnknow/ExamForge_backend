package com.examforge.analytics.controller;

import com.examforge.analytics.dto.PopularExamResponse;
import com.examforge.analytics.dto.PopularTestResponse;
import com.examforge.analytics.dto.RegistrationTrendPoint;
import com.examforge.analytics.dto.TestAnalyticsResponse;
import com.examforge.analytics.service.AnalyticsService;
import com.examforge.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin-only aggregate dashboards. Every response here is aggregate data
 * (counts, averages, totals) - no individual user's answers or per-user
 * score is ever returned by these endpoints; that stays behind the
 * owner-only attempt/result endpoints in the attempt and result modules.
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Admin - Analytics", description = "Aggregate dashboards: test performance, popularity, registration trends")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AnalyticsAdminController {

    private final AnalyticsService analyticsService;

    @GetMapping("/tests/{testId}")
    @Operation(summary = "Attempt counts, completion rate, average score/accuracy for one test")
    public ApiResponse<TestAnalyticsResponse> testAnalytics(@PathVariable UUID testId) {
        return ApiResponse.success(analyticsService.getTestAnalytics(testId));
    }

    @GetMapping("/popular-tests")
    @Operation(summary = "Top tests by attempt count")
    public ApiResponse<List<PopularTestResponse>> popularTests(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(analyticsService.getPopularTests(limit));
    }

    @GetMapping("/popular-exams")
    @Operation(summary = "Top exams by attempt count (aggregated across their tests)")
    public ApiResponse<List<PopularExamResponse>> popularExams(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(analyticsService.getPopularExams(limit));
    }

    @GetMapping("/user-registrations")
    @Operation(summary = "Daily user registration counts for the last N days")
    public ApiResponse<List<RegistrationTrendPoint>> registrationTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(analyticsService.getRegistrationTrend(days));
    }
}
