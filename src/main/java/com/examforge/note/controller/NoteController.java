package com.examforge.note.controller;

import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.common.response.ApiResponse;
import com.examforge.note.dto.NoteRequest;
import com.examforge.note.dto.NoteResponse;
import com.examforge.note.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/questions/{questionId}/notes")
@RequiredArgsConstructor
@Tag(name = "Notes", description = "A user's personal notes on a question")
@SecurityRequirement(name = "bearerAuth")
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    @Operation(summary = "Get the current user's note on this question, if any")
    public ApiResponse<NoteResponse> get(@PathVariable UUID questionId) {
        NoteResponse response = noteService.getMyNote(questionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Note", questionId));
        return ApiResponse.success(response);
    }

    @PostMapping
    @Operation(summary = "Create the current user's note on this question")
    public ApiResponse<NoteResponse> create(@PathVariable UUID questionId, @Valid @RequestBody NoteRequest request) {
        return ApiResponse.success("Note saved", noteService.saveMyNote(questionId, request.getNoteText()));
    }

    @PutMapping
    @Operation(summary = "Update the current user's note on this question")
    public ApiResponse<NoteResponse> update(@PathVariable UUID questionId, @Valid @RequestBody NoteRequest request) {
        return ApiResponse.success("Note updated", noteService.saveMyNote(questionId, request.getNoteText()));
    }

    @DeleteMapping
    @Operation(summary = "Delete the current user's note on this question")
    public ApiResponse<Void> delete(@PathVariable UUID questionId) {
        noteService.deleteMyNote(questionId);
        return ApiResponse.success("Note deleted", null);
    }
}
