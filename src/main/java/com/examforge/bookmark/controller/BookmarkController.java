package com.examforge.bookmark.controller;

import com.examforge.bookmark.dto.BookmarkRequest;
import com.examforge.bookmark.dto.BookmarkResponse;
import com.examforge.bookmark.service.BookmarkService;
import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmarks", description = "A user's saved questions")
@SecurityRequirement(name = "bearerAuth")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping
    @Operation(summary = "List the current user's bookmarks")
    public ApiResponse<PageResponse<BookmarkResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(bookmarkService.getMyBookmarks(pageable)));
    }

    @PostMapping
    @Operation(summary = "Bookmark a question")
    public ResponseEntity<ApiResponse<BookmarkResponse>> add(@Valid @RequestBody BookmarkRequest request) {
        BookmarkResponse response = bookmarkService.addBookmark(request.getQuestionId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Question bookmarked", response));
    }

    @DeleteMapping("/{questionId}")
    @Operation(summary = "Remove a bookmark")
    public ApiResponse<Void> remove(@PathVariable UUID questionId) {
        bookmarkService.removeBookmark(questionId);
        return ApiResponse.success("Bookmark removed", null);
    }
}
