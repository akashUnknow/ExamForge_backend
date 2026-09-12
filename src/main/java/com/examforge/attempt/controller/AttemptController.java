package com.examforge.attempt.controller;

import com.examforge.attempt.dto.AnswerRequest;
import com.examforge.attempt.dto.AttemptResponse;
import com.examforge.attempt.dto.AttemptResultResponse;
import com.examforge.attempt.dto.QuestionAttemptResponse;
import com.examforge.attempt.service.AttemptService;
import com.examforge.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Test-taking flow. Every endpoint here requires authentication (enforced
 * by SecurityConfig's default authenticated() rule - no special role
 * needed, any logged-in user can take a test) and every attempt is
 * strictly scoped to its owner - see findOwnedAttempt() in
 * AttemptServiceImpl, which returns 404 rather than 403 for someone
 * else's attempt id, to avoid confirming it exists.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Attempts", description = "Test-taking: start, answer, submit, and review")
@SecurityRequirement(name = "bearerAuth")
public class AttemptController {

    private final AttemptService attemptService;

    @PostMapping("/api/v1/tests/{testId}/attempts")
    @Operation(summary = "Start a new attempt, or resume an existing in-progress one")
    public ResponseEntity<ApiResponse<AttemptResponse>> startAttempt(@PathVariable UUID testId) {
        AttemptResponse response = attemptService.startAttempt(testId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Attempt started", response));
    }

    @GetMapping("/api/v1/attempts/{attemptId}/questions")
    @Operation(summary = "Get the answer-free question list for an in-progress attempt")
    public ApiResponse<List<QuestionAttemptResponse>> getQuestions(@PathVariable UUID attemptId) {
        return ApiResponse.success(attemptService.getQuestions(attemptId));
    }

    @PostMapping("/api/v1/attempts/{attemptId}/answers")
    @Operation(summary = "Save (upsert) an answer for one question")
    public ApiResponse<Void> saveAnswer(@PathVariable UUID attemptId, @Valid @RequestBody AnswerRequest request) {
        attemptService.saveAnswer(attemptId, request);
        return ApiResponse.success("Answer saved", null);
    }

    @PostMapping("/api/v1/attempts/{attemptId}/submit")
    @Operation(summary = "Submit the attempt - backend computes the final score")
    public ApiResponse<AttemptResultResponse> submit(@PathVariable UUID attemptId) {
        return ApiResponse.success("Attempt submitted", attemptService.submit(attemptId));
    }

    @GetMapping("/api/v1/attempts/{attemptId}/result")
    @Operation(summary = "Get the result - only available after submission")
    public ApiResponse<AttemptResultResponse> getResult(@PathVariable UUID attemptId) {
        return ApiResponse.success(attemptService.getResult(attemptId));
    }
}
