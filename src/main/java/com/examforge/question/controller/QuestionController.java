package com.examforge.question.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.question.domain.QuestionStatus;
import com.examforge.question.domain.QuestionType;
import com.examforge.question.dto.QuestionRequest;
import com.examforge.question.dto.QuestionResponse;
import com.examforge.question.service.QuestionService;
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
 * Question authoring &amp; review. Not a public catalog - only
 * CONTENT_CREATOR, REVIEWER, ADMIN, and SUPER_ADMIN ever call this. End
 * users encounter question content exclusively through the Test/Attempt
 * endpoints (a later phase), which must serve a different, answer-free
 * response shape - see the warning on {@link QuestionResponse}.
 */
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "Questions", description = "Question bank authoring and review")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('CONTENT_CREATOR', 'REVIEWER', 'ADMIN', 'SUPER_ADMIN')")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    @Operation(summary = "Search questions")
    public ApiResponse<PageResponse<QuestionResponse>> search(
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) ExamDifficulty difficulty,
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(
                questionService.search(topicId, subjectId, status, difficulty, type, keyword, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a question by id, including its answer key")
    public ApiResponse<QuestionResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(questionService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new question (starts in DRAFT)")
    @PreAuthorize("hasAnyRole('CONTENT_CREATOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionResponse>> create(@Valid @RequestBody QuestionRequest request) {
        QuestionResponse response = questionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Question created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a question (only while DRAFT or REJECTED, and only by its creator or an admin)")
    @PreAuthorize("hasAnyRole('CONTENT_CREATOR', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuestionResponse> update(@PathVariable UUID id, @Valid @RequestBody QuestionRequest request) {
        return ApiResponse.success("Question updated successfully", questionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a question (only while DRAFT)")
    @PreAuthorize("hasAnyRole('CONTENT_CREATOR', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        questionService.delete(id);
        return ApiResponse.success("Question deleted successfully", null);
    }
}
