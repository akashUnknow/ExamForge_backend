package com.examforge.study.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.study.domain.MaterialType;
import com.examforge.study.dto.StudyMaterialResponse;
import com.examforge.study.service.StudyMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/study-materials")
@RequiredArgsConstructor
@Tag(name = "Admin - Study Materials", description = "Admin study material management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class StudyMaterialAdminController {

    private final StudyMaterialService studyMaterialService;

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Upload a new study material file with metadata")
    public ResponseEntity<ApiResponse<StudyMaterialResponse>> upload(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam MaterialType type,
            @RequestParam(required = false) UUID examId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam("file") MultipartFile file) throws IOException {

        StudyMaterialResponse response = studyMaterialService.upload(title, description, type, examId, subjectId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Study material uploaded successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a study material (soft delete metadata + removes the underlying file)")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        studyMaterialService.delete(id);
        return ApiResponse.success("Study material deleted successfully", null);
    }
}
