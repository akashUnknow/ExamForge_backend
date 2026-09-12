package com.examforge.testseries.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.test.dto.TestPaperResponse;
import com.examforge.testseries.dto.TestSeriesResponse;
import com.examforge.testseries.service.TestSeriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test-series")
@RequiredArgsConstructor
@Tag(name = "Test Series", description = "Public test series catalog")
public class TestSeriesController {

    private final TestSeriesService testSeriesService;

    @GetMapping
    @Operation(summary = "List published test series")
    public ApiResponse<PageResponse<TestSeriesResponse>> list(
            @RequestParam(required = false) UUID examId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(testSeriesService.searchPublic(examId, keyword, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a published test series by id")
    public ApiResponse<TestSeriesResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(testSeriesService.getPublicById(id));
    }

    @GetMapping("/{id}/tests")
    @Operation(summary = "List the tests bundled in this series",
            description = "Metadata only - actual attempt access is still governed by each test's own free/subscription gate")
    public ApiResponse<List<TestPaperResponse>> tests(@PathVariable UUID id) {
        return ApiResponse.success(testSeriesService.getPublicTests(id));
    }
}
