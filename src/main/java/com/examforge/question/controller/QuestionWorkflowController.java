package com.examforge.question.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.question.dto.QuestionResponse;
import com.examforge.question.dto.RejectRequest;
import com.examforge.question.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Content workflow transitions: DRAFT/REJECTED -&gt; IN_REVIEW -&gt;
 * APPROVED -&gt; PUBLISHED, with REJECTED and ARCHIVED as side branches.
 * Each transition is independently role-gated; see {@code QuestionServiceImpl}
 * for the ownership rules layered on top (a CONTENT_CREATOR may only
 * submit their own drafts, a REVIEWER may not approve/reject their own
 * submissions).
 */
@RestController
@RequestMapping("/api/v1/questions/{id}")
@RequiredArgsConstructor
@Tag(name = "Questions", description = "Question workflow transitions")
@SecurityRequirement(name = "bearerAuth")
public class QuestionWorkflowController {

    private final QuestionService questionService;

    @PostMapping("/submit-for-review")
    @Operation(summary = "Submit a DRAFT or REJECTED question for review")
    @PreAuthorize("hasAnyRole('CONTENT_CREATOR', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuestionResponse> submitForReview(@PathVariable UUID id) {
        return ApiResponse.success("Question submitted for review", questionService.submitForReview(id));
    }

    @PostMapping("/approve")
    @Operation(summary = "Approve a question that is IN_REVIEW")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuestionResponse> approve(@PathVariable UUID id) {
        return ApiResponse.success("Question approved", questionService.approve(id));
    }

    @PostMapping("/reject")
    @Operation(summary = "Reject a question that is IN_REVIEW")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuestionResponse> reject(@PathVariable UUID id, @Valid @RequestBody(required = false) RejectRequest request) {
        String reason = request != null ? request.getReason() : null;
        return ApiResponse.success("Question rejected", questionService.reject(id, reason));
    }

    @PostMapping("/publish")
    @Operation(summary = "Publish a question that is APPROVED")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuestionResponse> publish(@PathVariable UUID id) {
        return ApiResponse.success("Question published", questionService.publish(id));
    }

    @PostMapping("/archive")
    @Operation(summary = "Archive a PUBLISHED, APPROVED, or REJECTED question")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuestionResponse> archive(@PathVariable UUID id) {
        return ApiResponse.success("Question archived", questionService.archive(id));
    }
}
