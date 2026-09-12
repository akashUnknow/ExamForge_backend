package com.examforge.exam.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.exam.dto.ExamCategoryRequest;
import com.examforge.exam.dto.ExamCategoryResponse;
import com.examforge.exam.service.ExamCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/exam-categories")
@RequiredArgsConstructor
@Tag(name = "Admin - Exam Categories", description = "Admin exam category management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class ExamCategoryAdminController {

    private final ExamCategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create an exam category")
    public ResponseEntity<ApiResponse<ExamCategoryResponse>> create(@Valid @RequestBody ExamCategoryRequest request) {
        ExamCategoryResponse response = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Exam category created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an exam category")
    public ApiResponse<ExamCategoryResponse> update(@PathVariable UUID id, @Valid @RequestBody ExamCategoryRequest request) {
        return ApiResponse.success("Exam category updated successfully", categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an exam category (soft delete, blocked if exams still reference it)")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ApiResponse.success("Exam category deleted successfully", null);
    }
}
