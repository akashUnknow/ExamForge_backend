package com.examforge.result.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.result.dto.AttemptSummaryResponse;
import com.examforge.result.service.ResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Results", description = "A user's own attempt history")
@SecurityRequirement(name = "bearerAuth")
public class ResultController {

    private final ResultService resultService;

    @GetMapping("/api/v1/users/me/attempts")
    @Operation(summary = "The current user's attempt history across all tests")
    public ApiResponse<PageResponse<AttemptSummaryResponse>> myAttempts(
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(resultService.getMyAttempts(pageable)));
    }

    @GetMapping("/api/v1/tests/{testId}/attempts/me")
    @Operation(summary = "The current user's attempts on one specific test (retake history)")
    public ApiResponse<PageResponse<AttemptSummaryResponse>> myAttemptsForTest(
            @PathVariable UUID testId,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(resultService.getMyAttemptsForTest(testId, pageable)));
    }
}
