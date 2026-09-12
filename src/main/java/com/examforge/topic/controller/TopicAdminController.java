package com.examforge.topic.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.topic.dto.TopicRequest;
import com.examforge.topic.dto.TopicResponse;
import com.examforge.topic.service.TopicService;
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
@RequestMapping("/api/v1/admin/topics")
@RequiredArgsConstructor
@Tag(name = "Admin - Topics", description = "Admin topic management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class TopicAdminController {

    private final TopicService topicService;

    @GetMapping
    @Operation(summary = "List topics for a subject, regardless of exam status")
    public ApiResponse<PageResponse<TopicResponse>> list(
            @RequestParam UUID subjectId,
            @PageableDefault(size = 50, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(topicService.getAdminBySubject(subjectId, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a topic by id")
    public ApiResponse<TopicResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(topicService.getAdminById(id));
    }

    @PostMapping
    @Operation(summary = "Create a topic under a subject")
    public ResponseEntity<ApiResponse<TopicResponse>> create(@Valid @RequestBody TopicRequest request) {
        TopicResponse response = topicService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Topic created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a topic")
    public ApiResponse<TopicResponse> update(@PathVariable UUID id, @Valid @RequestBody TopicRequest request) {
        return ApiResponse.success("Topic updated successfully", topicService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a topic (soft delete)")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        topicService.delete(id);
        return ApiResponse.success("Topic deleted successfully", null);
    }
}
