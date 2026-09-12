package com.examforge.study.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.study.domain.MaterialType;
import com.examforge.study.dto.StudyMaterialResponse;
import com.examforge.study.service.StudyMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

/**
 * Public study material catalog and download. Metadata is served as JSON;
 * the actual bytes are streamed separately via /file so listing calls
 * stay lightweight.
 */
@RestController
@RequestMapping("/api/v1/study-materials")
@RequiredArgsConstructor
@Tag(name = "Study Materials", description = "Public study material catalog and download")
public class StudyMaterialController {

    private final StudyMaterialService studyMaterialService;

    @GetMapping
    @Operation(summary = "List study materials")
    public ApiResponse<PageResponse<StudyMaterialResponse>> list(
            @RequestParam(required = false) UUID examId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) MaterialType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(studyMaterialService.search(examId, subjectId, type, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get study material metadata by id")
    public ApiResponse<StudyMaterialResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(studyMaterialService.getById(id));
    }

    @GetMapping("/{id}/file")
    @Operation(summary = "Download the underlying file")
    public ResponseEntity<Resource> download(@PathVariable UUID id) throws IOException {
        StudyMaterialService.LoadedFile file = studyMaterialService.loadFile(id);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(file.contentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .body(file.resource());
    }
}
