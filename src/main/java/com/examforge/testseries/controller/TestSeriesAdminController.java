package com.examforge.testseries.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.test.domain.TestStatus;
import com.examforge.testseries.dto.TestSeriesRequest;
import com.examforge.testseries.dto.TestSeriesResponse;
import com.examforge.testseries.dto.TestSeriesTestAttachRequest;
import com.examforge.testseries.service.TestSeriesService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/test-series")
@RequiredArgsConstructor
@Tag(name = "Admin - Test Series", description = "Admin test series management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class TestSeriesAdminController {

    private final TestSeriesService testSeriesService;

    @GetMapping
    @Operation(summary = "List test series in any status")
    public ApiResponse<PageResponse<TestSeriesResponse>> list(
            @RequestParam(required = false) UUID examId,
            @RequestParam(required = false) TestStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(testSeriesService.searchAdmin(examId, status, keyword, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a test series by id, in any status")
    public ApiResponse<TestSeriesResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(testSeriesService.getAdminById(id));
    }

    @PostMapping
    @Operation(summary = "Create a test series")
    public ResponseEntity<ApiResponse<TestSeriesResponse>> create(@Valid @RequestBody TestSeriesRequest request) {
        TestSeriesResponse response = testSeriesService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Test series created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a test series")
    public ApiResponse<TestSeriesResponse> update(@PathVariable UUID id, @Valid @RequestBody TestSeriesRequest request) {
        return ApiResponse.success("Test series updated successfully", testSeriesService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a test series (blocked if tests are still attached)")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        testSeriesService.delete(id);
        return ApiResponse.success("Test series deleted successfully", null);
    }

    @PostMapping("/{id}/tests")
    @Operation(summary = "Attach a test to this series")
    public ApiResponse<Void> attachTest(@PathVariable UUID id, @Valid @RequestBody TestSeriesTestAttachRequest request) {
        testSeriesService.attachTest(id, request.getTestId(), request.getDisplayOrder());
        return ApiResponse.success("Test attached successfully", null);
    }

    @DeleteMapping("/{id}/tests/{testId}")
    @Operation(summary = "Detach a test from this series")
    public ApiResponse<Void> detachTest(@PathVariable UUID id, @PathVariable UUID testId) {
        testSeriesService.detachTest(id, testId);
        return ApiResponse.success("Test detached successfully", null);
    }
}
