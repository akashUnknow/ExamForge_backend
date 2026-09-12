package com.examforge.question.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.question.dto.QuestionTranslationRequest;
import com.examforge.question.dto.QuestionTranslationResponse;
import com.examforge.question.service.QuestionTranslationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/questions/{questionId}/translations")
@RequiredArgsConstructor
@Tag(name = "Questions", description = "Question translations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('CONTENT_CREATOR', 'REVIEWER', 'ADMIN', 'SUPER_ADMIN')")
public class QuestionTranslationController {

    private final QuestionTranslationService translationService;

    @GetMapping
    @Operation(summary = "List translations for a question")
    public ApiResponse<List<QuestionTranslationResponse>> list(@PathVariable UUID questionId) {
        return ApiResponse.success(translationService.getByQuestion(questionId));
    }

    @PostMapping
    @Operation(summary = "Add a translation for a question")
    @PreAuthorize("hasAnyRole('CONTENT_CREATOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionTranslationResponse>> create(
            @PathVariable UUID questionId, @Valid @RequestBody QuestionTranslationRequest request) {
        QuestionTranslationResponse response = translationService.create(questionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Translation added successfully", response));
    }

    @DeleteMapping("/{translationId}")
    @Operation(summary = "Delete a translation")
    @PreAuthorize("hasAnyRole('CONTENT_CREATOR', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID questionId, @PathVariable UUID translationId) {
        translationService.delete(questionId, translationId);
        return ApiResponse.success("Translation deleted successfully", null);
    }
}
