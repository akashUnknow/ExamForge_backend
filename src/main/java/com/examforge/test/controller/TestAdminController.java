package com.examforge.test.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.test.domain.TestStatus;
import com.examforge.test.dto.TestPaperRequest;
import com.examforge.test.dto.TestPaperResponse;
import com.examforge.test.service.TestPaperService;
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
@RequestMapping("/api/v1/admin/tests")
@RequiredArgsConstructor
@Tag(name = "Admin - Tests", description = "Admin test management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class TestAdminController {

    private final TestPaperService testService;

    @GetMapping
    @Operation(summary = "List tests in any status")
    public ApiResponse<PageResponse<TestPaperResponse>> list(
            @RequestParam(required = false) UUID examId,
            @RequestParam(required = false) TestStatus status,
            @RequestParam(required = false) Boolean free,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(testService.searchAdmin(examId, status, free, keyword, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a test by id, in any status")
    public ApiResponse<TestPaperResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(testService.getAdminById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new test")
    public ResponseEntity<ApiResponse<TestPaperResponse>> create(@Valid @RequestBody TestPaperRequest request) {
        TestPaperResponse response = testService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Test created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a test")
    public ApiResponse<TestPaperResponse> update(@PathVariable UUID id, @Valid @RequestBody TestPaperRequest request) {
        return ApiResponse.success("Test updated successfully", testService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a test (soft delete, blocked if sections/questions still attached)")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        testService.delete(id);
        return ApiResponse.success("Test deleted successfully", null);
    }
}
