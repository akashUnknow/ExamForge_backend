package com.examforge.test.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.test.dto.TestSectionRequest;
import com.examforge.test.dto.TestSectionResponse;
import com.examforge.test.service.TestSectionService;
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
@RequestMapping("/api/v1/admin/tests/{testId}/sections")
@RequiredArgsConstructor
@Tag(name = "Admin - Tests", description = "Admin test section management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class TestSectionAdminController {

    private final TestSectionService sectionService;

    @GetMapping
    @Operation(summary = "List sections for a test")
    public ApiResponse<List<TestSectionResponse>> list(@PathVariable UUID testId) {
        return ApiResponse.success(sectionService.getByTest(testId));
    }

    @PostMapping
    @Operation(summary = "Create a section under a test")
    public ResponseEntity<ApiResponse<TestSectionResponse>> create(
            @PathVariable UUID testId, @Valid @RequestBody TestSectionRequest request) {
        TestSectionResponse response = sectionService.create(testId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Section created successfully", response));
    }

    @PutMapping("/{sectionId}")
    @Operation(summary = "Update a section")
    public ApiResponse<TestSectionResponse> update(
            @PathVariable UUID testId, @PathVariable UUID sectionId, @Valid @RequestBody TestSectionRequest request) {
        return ApiResponse.success("Section updated successfully", sectionService.update(testId, sectionId, request));
    }

    @DeleteMapping("/{sectionId}")
    @Operation(summary = "Delete a section (blocked if questions are still assigned to it)")
    public ApiResponse<Void> delete(@PathVariable UUID testId, @PathVariable UUID sectionId) {
        sectionService.delete(testId, sectionId);
        return ApiResponse.success("Section deleted successfully", null);
    }
}
