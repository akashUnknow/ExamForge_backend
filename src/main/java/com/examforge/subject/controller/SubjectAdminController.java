package com.examforge.subject.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.subject.dto.SubjectRequest;
import com.examforge.subject.dto.SubjectResponse;
import com.examforge.subject.service.SubjectService;
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
@RequestMapping("/api/v1/admin/subjects")
@RequiredArgsConstructor
@Tag(name = "Admin - Subjects", description = "Admin subject management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class SubjectAdminController {

    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "List subjects for an exam, in any exam status")
    public ApiResponse<PageResponse<SubjectResponse>> list(
            @RequestParam UUID examId,
            @PageableDefault(size = 50, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(subjectService.getAdminByExam(examId, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a subject by id")
    public ApiResponse<SubjectResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(subjectService.getAdminById(id));
    }

    @PostMapping
    @Operation(summary = "Create a subject under an exam")
    public ResponseEntity<ApiResponse<SubjectResponse>> create(@Valid @RequestBody SubjectRequest request) {
        SubjectResponse response = subjectService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subject created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a subject")
    public ApiResponse<SubjectResponse> update(@PathVariable UUID id, @Valid @RequestBody SubjectRequest request) {
        return ApiResponse.success("Subject updated successfully", subjectService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a subject (soft delete, blocked if topics still reference it)")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        subjectService.delete(id);
        return ApiResponse.success("Subject deleted successfully", null);
    }
}
