package com.examforge.exam.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.exam.dto.ExamRequest;
import com.examforge.exam.dto.ExamResponse;
import com.examforge.exam.service.ExamService;
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

/**
 * Admin exam management. All statuses visible; write operations
 * require ADMIN or SUPER_ADMIN (enforced at the URL-pattern level by
 * SecurityConfig, and again here via @PreAuthorize as defense in depth).
 */
@RestController
@RequestMapping("/api/v1/admin/exams")
@RequiredArgsConstructor
@Tag(name = "Admin - Exams", description = "Admin exam management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class ExamAdminController {

    private final ExamService examService;

    @GetMapping
    @Operation(summary = "List exams in any status")
    public ApiResponse<PageResponse<ExamResponse>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) ExamDifficulty difficulty,
            @RequestParam(required = false) ExamStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(
                PageResponse.from(examService.searchAdmin(categoryId, difficulty, status, keyword, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an exam by id, in any status")
    public ApiResponse<ExamResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(examService.getAdminById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new exam")
    public ResponseEntity<ApiResponse<ExamResponse>> create(@Valid @RequestBody ExamRequest request) {
        ExamResponse response = examService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Exam created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing exam")
    public ApiResponse<ExamResponse> update(@PathVariable UUID id, @Valid @RequestBody ExamRequest request) {
        return ApiResponse.success("Exam updated successfully", examService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an exam (soft delete)")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        examService.delete(id);
        return ApiResponse.success("Exam deleted successfully", null);
    }
}
